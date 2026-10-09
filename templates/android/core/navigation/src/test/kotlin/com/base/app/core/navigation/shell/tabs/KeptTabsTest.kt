package com.base.app.core.navigation.shell.tabs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import com.base.app.core.designsystem.foundation.LocalTabVisible
import com.base.app.core.navigation.ROBOLECTRIC_SDK
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class KeptTabsTest {

    @get:Rule
    val rule = createComposeRule()

    private class TabProbe {
        var compositions = 0
        var disposals = 0
        var measures = 0
        var lifecycle: Lifecycle? = null
        var dispatcher: NavigationEventDispatcher? = null
        var visible = true
    }

    private val probes = List(2) { TabProbe() }
    private var selected by mutableIntStateOf(0)
    private var generation by mutableIntStateOf(1)
    private var inset by mutableStateOf(0.dp)

    private fun showTabs() {
        rule.setContent {
            KeptTabs(selected = selected, generation = generation, modifier = Modifier.padding(inset)) { index ->
                val probe = probes[index]
                remember { probe.compositions++ }
                DisposableEffect(Unit) { onDispose { probe.disposals++ } }
                probe.lifecycle = LocalLifecycleOwner.current.lifecycle
                probe.dispatcher = LocalNavigationEventDispatcherOwner.current!!.navigationEventDispatcher
                probe.visible = LocalTabVisible.current
                Box(
                    Modifier.fillMaxSize().layout { measurable, constraints ->
                        probe.measures++
                        val placeable = measurable.measure(constraints)
                        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                    },
                )
            }
        }
    }

    @Test
    fun `a tab left behind stays composed, unmeasured, created, and deaf to Back`() {
        showTabs()
        selected = 1
        rule.waitForIdle()
        selected = 0
        rule.waitForIdle()
        selected = 1
        rule.waitForIdle()
        val hidden = probes[0]
        val hiddenMeasures = hidden.measures
        val shownMeasures = probes[1].measures

        // New constraints for the whole host: only the tab in front is measured again.
        inset = 8.dp
        rule.waitForIdle()

        assertEquals("composed once, never rebuilt", 1, hidden.compositions)
        assertEquals(0, hidden.disposals)
        assertEquals(Lifecycle.State.CREATED, hidden.lifecycle!!.currentState)
        assertFalse(hidden.dispatcher!!.isEnabled)
        assertFalse(hidden.visible)
        assertEquals(hiddenMeasures, hidden.measures)
        assertTrue(probes[1].measures > shownMeasures)
        assertEquals(Lifecycle.State.RESUMED, probes[1].lifecycle!!.currentState)
        assertTrue(probes[1].dispatcher!!.isEnabled)
    }

    @Test
    fun `a new generation drops every kept tab`() {
        showTabs()
        selected = 1
        rule.waitForIdle()

        generation = 2
        rule.waitForIdle()

        assertEquals(1, probes[0].disposals)
        assertEquals(1, probes[1].disposals)
        assertEquals("the new session composes only the tab in front", 1, probes[0].compositions)
        assertEquals(2, probes[1].compositions)
    }
}
