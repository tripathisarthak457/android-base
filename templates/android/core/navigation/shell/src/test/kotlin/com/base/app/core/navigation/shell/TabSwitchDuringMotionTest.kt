package com.base.app.core.navigation.shell

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.navigation.AppNavigator
import com.base.app.core.navigation.NavCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A tab tapped while the selected tab is in the middle of a push or a pop (A3.4a): the switch is
 * instant, the tab left behind finishes its slide out of sight, and coming back to it shows the
 * slide's end, never a frame from its middle.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK], qualifiers = "w411dp-h891dp")
class TabSwitchDuringMotionTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val registry = testRegistry()
    private lateinit var state: ShellState

    private fun showShell() {
        rule.setContent {
            AppTheme {
                val shell =
                    rememberShellState(testTabs, registry, testSerialization, generation = 1)
                state = shell
                AppShell(
                    tabs = testTabs,
                    state = shell,
                    registry = registry,
                    navigator = AppNavigator(),
                )
            }
        }
        rule.waitForIdle()
    }

    @Test
    fun `a tab left in the middle of a push shows the pushed screen in place when it comes back`() {
        showShell()
        rule.mainClock.autoAdvance = false
        rule.runOnIdle { state.submit(NavCommand.Navigate(Post(1))) }
        repeat(MID_SLIDE_FRAMES) { rule.mainClock.advanceTimeByFrame() }
        rule.runOnIdle { assertTrue("the push is still sliding", state.tabs[0].gate.moving) }

        rule.runOnIdle { state.select(1) }
        rule.mainClock.advanceTimeByFrame()
        rule.runOnIdle { assertEquals(1, state.selectedIndex) }
        rule.mainClock.advanceTimeBy(SLIDE_DONE_MS)
        rule.runOnIdle {
            assertFalse("the push ended while its tab was hidden", state.tabs[0].gate.moving)
        }

        rule.runOnIdle { state.select(0) }
        rule.mainClock.advanceTimeByFrame()
        rule.runOnIdle { assertFalse(state.tabs[0].gate.moving) }
        rule.onNodeWithTag(
            Post(1).toString(),
            useUnmergedTree = true,
        ).assertLeftPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun `a tab left in the middle of a pop shows its root in place when it comes back`() {
        showShell()
        rule.runOnIdle { state.submit(NavCommand.Navigate(Post(1))) }
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        repeat(MID_SLIDE_FRAMES) { rule.mainClock.advanceTimeByFrame() }
        rule.runOnIdle { assertTrue("the pop is still sliding", state.tabs[0].gate.moving) }

        rule.runOnIdle { state.select(1) }
        rule.mainClock.advanceTimeBy(SLIDE_DONE_MS)
        rule.runOnIdle {
            assertFalse("the pop ended while its tab was hidden", state.tabs[0].gate.moving)
            assertEquals(listOf(FeedTab), state.tabs[0].keys)
        }

        rule.runOnIdle { state.select(0) }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithTag(Post(1).toString(), useUnmergedTree = true).assertDoesNotExist()
        rule.onNodeWithTag(
            FeedTab.toString(),
            useUnmergedTree = true,
        ).assertLeftPositionInRootIsEqualTo(0.dp)
    }
}

/** Far enough into a slide that it is moving, well short of its end. */
private const val MID_SLIDE_FRAMES = 4
private const val SLIDE_DONE_MS = 2_000L
