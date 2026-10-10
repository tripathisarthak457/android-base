package com.base.app.core.navigation.recomposition

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEvent
import com.base.app.core.common.navigation.AppNavKey
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.navigation.AppNavigator
import com.base.app.core.navigation.AppShell
import com.base.app.core.navigation.NavCommand
import com.base.app.core.navigation.Post
import com.base.app.core.navigation.ROBOLECTRIC_SDK
import com.base.app.core.navigation.ShellState
import com.base.app.core.navigation.back.submit
import com.base.app.core.navigation.rememberShellState
import com.base.app.core.navigation.testRegistry
import com.base.app.core.navigation.testSerialization
import com.base.app.core.navigation.testTabs
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The shell's journeys compose only on their first and last frames: a tab switch, a push, a pop, a
 * back gesture and a burst of Back presses move layers and redraw in between, and the screens
 * they move are not composed again at all.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK], qualifiers = "w411dp-h891dp")
class ShellRecompositionTest {

    @get:Rule(order = 0)
    val counter = RecompositionCounter()

    @get:Rule(order = 1)
    val rule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var state: ShellState
    private val input = DirectNavigationEventInput()

    private fun showShell() {
        val registry = testRegistry { key -> ProbeScreen(key) }
        rule.setContent {
            AppTheme {
                val shell = rememberShellState(testTabs, registry, testSerialization, generation = 1)
                state = shell
                AppShell(tabs = testTabs, state = shell, registry = registry, navigator = AppNavigator())
            }
        }
        rule.runOnIdle { rule.activity.navigationEventDispatcher.addInput(input) }
        // The other tabs are built while the shell is idle; journeys start once that is over.
        rule.mainClock.advanceTimeBy(WARM_UP_WAIT_MILLIS)
        rule.waitForIdle()
    }

    /** Runs [action] on the UI thread with the clock stopped, then steps every frame it causes. */
    private fun journey(action: () -> Unit): List<Map<String, Int>> {
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        counter.reset()
        rule.runOnUiThread(action)
        val frames = rule.framesUntilIdle(counter)
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        return frames
    }

    @Test
    fun `a switch to a visited tab composes neither tab's screens`() {
        showShell()
        rule.onNodeWithTag("tab_1").performSemanticsAction(SemanticsActions.OnClick)
        rule.onNodeWithTag("tab_0").performSemanticsAction(SemanticsActions.OnClick)

        val frames = journey { rule.onNodeWithTag("tab_1").performSemanticsAction(SemanticsActions.OnClick) }

        frames.assertQuietBetweenEnds()
        assertEquals(0, frames.sumOf { it.count("ProbeScreen") })
    }

    @Test
    fun `a first visit to a tab built while idle composes none of its screens`() {
        showShell()

        val frames = journey { rule.onNodeWithTag("tab_1").performSemanticsAction(SemanticsActions.OnClick) }

        frames.assertQuietBetweenEnds()
        assertEquals(0, frames.sumOf { it.count("ProbeScreen") })
    }

    @Test
    fun `a push composes the new screen once and nothing while it slides`() {
        showShell()

        val frames = journey { state.submit(NavCommand.Navigate(Post(1))) }

        frames.assertQuietBetweenEnds()
        assertEquals(1, frames.sumOf { it.count("ProbeScreen") })
        assertEquals(0, frames.sumOf { it.count("AppTabBar") })
    }

    @Test
    fun `a pop composes neither the leaving screen nor the one it returns to`() {
        showShell()
        rule.runOnIdle { state.submit(NavCommand.Navigate(Post(1))) }

        val frames = journey { rule.activity.onBackPressedDispatcher.onBackPressed() }

        frames.assertQuietBetweenEnds()
        assertEquals(0, frames.sumOf { it.count("ProbeScreen") })
    }

    @Test
    fun `a back gesture composes nothing while the card follows the finger`() {
        showShell()
        rule.runOnIdle { state.submit(NavCommand.Navigate(Post(1))) }
        rule.waitForIdle()
        rule.runOnIdle { input.backStarted(event(0f)) }
        rule.waitForIdle()

        val frames = journey {
            for (step in 1..30) input.backProgressed(event(step / 40f))
        }
        rule.runOnIdle { input.backCancelled() }

        assertEquals(0, frames.sumOf { it.values.sum() })
    }

    @Test
    fun `three Back presses during a pop compose what one pop does`() {
        showShell()
        // Three screens on the root: every page of the stack is kept composed.
        repeat(3) {
            rule.runOnIdle { state.submit(NavCommand.Navigate(Post(it))) }
            rule.waitForIdle()
        }

        val frames = journey { repeat(3) { rule.activity.onBackPressedDispatcher.onBackPressed() } }

        assertEquals(0, frames.sumOf { it.count("ProbeScreen") })
        rule.runOnIdle { assertEquals(listOf(testTabs[0].key), state.current.keys) }
    }

    private fun Map<String, Int>.count(name: String): Int =
        entries.sumOf { (fq, n) -> if (fq == name || fq.endsWith(".$name")) n else 0 }

    private fun event(progress: Float) = NavigationEvent(
        swipeEdge = NavigationEvent.EDGE_LEFT,
        progress = progress,
        touchX = progress * 400f,
        touchY = 600f,
        frameTimeMillis = rule.mainClock.currentTime,
    )
}

/** A screen that is its key, and a name the counter can find. */
@Composable
private fun ProbeScreen(key: AppNavKey) {
    Box(Modifier.fillMaxSize().testTag(key.toString())) { BasicText(key.toString()) }
}

/** Past the tab warm-up's wait for the first tab to settle, with room for its slices. */
private const val WARM_UP_WAIT_MILLIS = 2_000L
