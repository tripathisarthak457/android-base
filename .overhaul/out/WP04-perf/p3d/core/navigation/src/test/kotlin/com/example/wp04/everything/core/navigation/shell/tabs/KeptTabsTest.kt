package com.example.wp04.everything.core.navigation.shell.tabs

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import com.example.wp04.everything.core.designsystem.foundation.layout.LocalTabVisible
import com.example.wp04.everything.core.designsystem.foundation.layout.TabVisibility
import com.example.wp04.everything.core.navigation.ROBOLECTRIC_SDK
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
        var visibility: TabVisibility? = null
    }

    private val probes = List(2) { TabProbe() }
    private var selected by mutableIntStateOf(0)
    private var generation by mutableIntStateOf(1)
    private var inset by mutableStateOf(0.dp)

    private fun showTabs(warmUp: List<Int> = emptyList()) {
        rule.setContent {
            KeptTabs(selected = selected, generation = generation, modifier = Modifier.padding(inset), warmUp = warmUp) { index ->
                val probe = probes[index]
                remember { probe.compositions++ }
                DisposableEffect(Unit) { onDispose { probe.disposals++ } }
                probe.lifecycle = LocalLifecycleOwner.current.lifecycle
                probe.dispatcher = LocalNavigationEventDispatcherOwner.current!!.navigationEventDispatcher
                probe.visibility = LocalTabVisible.current
                Box(
                    Modifier.fillMaxSize().testTag("tab$index").layout { measurable, constraints ->
                        probe.measures++
                        val placeable = measurable.measure(constraints)
                        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                    },
                )
            }
        }
    }

    @Test
    fun `a tab left behind stays composed, created, out of sight and deaf to Back`() {
        showTabs()
        selected = 1
        rule.waitForIdle()
        selected = 0
        rule.waitForIdle()
        selected = 1
        rule.waitForIdle()
        val hidden = probes[0]

        assertEquals("composed once, never rebuilt", 1, hidden.compositions)
        assertEquals(0, hidden.disposals)
        assertEquals(Lifecycle.State.CREATED, hidden.lifecycle!!.currentState)
        assertFalse(hidden.dispatcher!!.isEnabled)
        assertFalse(hidden.visibility!!.isVisible)
        rule.onNodeWithTag("tab0").assertDoesNotExist()
        assertEquals(Lifecycle.State.RESUMED, probes[1].lifecycle!!.currentState)
        assertTrue(probes[1].dispatcher!!.isEnabled)
        assertTrue(probes[1].visibility!!.isVisible)
        rule.onNodeWithTag("tab1").assertIsDisplayed()
    }

    @Test
    fun `switching back to a kept tab lays nothing out again`() {
        showTabs()
        selected = 1
        rule.waitForIdle()
        val measures = probes.map { it.measures }

        selected = 0
        rule.waitForIdle()
        selected = 1
        rule.waitForIdle()

        assertEquals(measures, probes.map { it.measures })
    }

    @Test
    fun `a hidden tab still follows the window, so it is ready when shown`() {
        showTabs()
        selected = 1
        rule.waitForIdle()
        val hiddenMeasures = probes[0].measures

        inset = 8.dp
        rule.waitForIdle()

        assertTrue(probes[0].measures > hiddenMeasures)
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

    @Test
    fun `a tab to warm up is built while idle, so its first visit builds and lays out nothing`() {
        showTabs(warmUp = listOf(0, 1))
        rule.mainClock.advanceTimeBy(WARM_UP_WAIT_MILLIS)
        rule.waitForIdle()
        val warmed = probes[1]

        assertEquals("built once, out of sight", 1, warmed.compositions)
        assertEquals(Lifecycle.State.CREATED, warmed.lifecycle!!.currentState)
        assertFalse(warmed.visibility!!.isVisible)
        rule.onNodeWithTag("tab1").assertDoesNotExist()
        val measures = warmed.measures
        assertTrue("laid out parked", measures > 0)

        selected = 1
        rule.waitForIdle()

        assertEquals(1, warmed.compositions)
        assertEquals(measures, warmed.measures)
        rule.onNodeWithTag("tab1").assertIsDisplayed()
    }

    private companion object {
        /** Past the warm-up's wait for the first tab to settle, with room for its slices. */
        const val WARM_UP_WAIT_MILLIS = 2_000L
    }
}
