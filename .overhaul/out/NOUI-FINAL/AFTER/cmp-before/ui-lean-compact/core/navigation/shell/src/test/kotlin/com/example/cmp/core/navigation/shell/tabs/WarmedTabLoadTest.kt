package com.example.cmp.core.navigation.shell.tabs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.cmp.core.mvi.MviViewModel
import com.example.cmp.core.mvi.UiEffect
import com.example.cmp.core.mvi.UiEvent
import com.example.cmp.core.mvi.UiState
import com.example.cmp.core.navigation.shell.ROBOLECTRIC_SDK
import com.example.cmp.core.ui.MviScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Where a screen's first load meets the shell's tab warm-up: a tab built ahead of its first visit
 * is held at CREATED, and a ViewModel that loads on its first observation must wait for the visit.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class WarmedTabLoadTest {

    @get:Rule
    val rule = createComposeRule()

    private data class Loads(val count: Int = 0) : UiState

    private sealed interface NoEvent : UiEvent

    private sealed interface NoEffect : UiEffect

    /** Loads the way every screen's ViewModel does: once, when the screen first watches its state. */
    private class LoadingViewModel : MviViewModel<Loads, NoEvent, NoEffect>(Loads()) {
        var loads = 0
            private set

        init {
            onFirstObserved { loads++ }
        }

        override fun handleEvent(event: NoEvent) = Unit
    }

    private val viewModels = List(2) { LoadingViewModel() }
    private val built = IntArray(2)
    private var selected by mutableIntStateOf(0)

    @Test
    fun `a warmed tab loads when it is first shown, and once`() {
        rule.setContent {
            KeptTabs(selected = { selected }, generation = 1, warmUp = listOf(0, 1)) { index ->
                remember { built[index]++ }
                // As a route does: MviScreen collects the state with the tab's lifecycle.
                MviScreen(viewModel = viewModels[index]) { _, _ -> Box(Modifier.fillMaxSize()) }
            }
        }
        rule.mainClock.advanceTimeBy(WARM_UP_WAIT_MILLIS)
        rule.waitForIdle()

        assertEquals("the tab in front loads", 1, viewModels[0].loads)
        assertEquals("the other tab is built ahead of its visit", 1, built[1])
        assertEquals("and has not loaded", 0, viewModels[1].loads)

        selected = 1
        rule.waitForIdle()
        selected = 0
        rule.waitForIdle()
        selected = 1
        rule.waitForIdle()

        assertEquals(1, viewModels[0].loads)
        assertEquals("shown, it loads once however often it comes back", 1, viewModels[1].loads)
        assertEquals("from the build it was warmed in", 1, built[1])
    }

    private companion object {
        /** Past the warm-up's wait for the first tab to settle, with room for its slices. */
        const val WARM_UP_WAIT_MILLIS = 2_000L
    }
}
