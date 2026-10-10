package com.base.app.core.navigation.shell

import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.base.app.core.common.navigation.AppNavKey
import com.base.app.core.designsystem.foundation.layout.ChromeInset
import com.base.app.core.designsystem.foundation.layout.LocalBottomChrome
import com.base.app.core.designsystem.foundation.layout.LocalScreenBottomBar
import com.base.app.core.designsystem.foundation.layout.LocalTabBarVisible
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.navigation.AppNavigator
import com.base.app.core.navigation.AppShell
import com.base.app.core.navigation.Compose
import com.base.app.core.navigation.FeedTab
import com.base.app.core.navigation.NavCommand
import com.base.app.core.navigation.Player
import com.base.app.core.navigation.Post
import com.base.app.core.navigation.ProfileTab
import com.base.app.core.navigation.ROBOLECTRIC_SDK
import com.base.app.core.navigation.Screen
import com.base.app.core.navigation.ShellConfig
import com.base.app.core.navigation.ShellState
import com.base.app.core.navigation.back.submit
import com.base.app.core.navigation.shell
import com.base.app.core.navigation.testRegistry
import com.base.app.core.navigation.testTabs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private val ACTION_BAR = 72.dp

/**
 * The layering contract: content < tab bar < presented sheets and covers. Presented screens win
 * every touch and every Back while they are up, and the bottom chrome is written by the shell alone.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK], qualifiers = "w411dp-h891dp")
class ChromeLayeringTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private var coverTaps = 0
    private var coverSawBar = true
    private val bottomChrome = ChromeInset()

    private val registry = testRegistry { key ->
        when (key) {
            Compose -> {
                coverSawBar = LocalTabBarVisible.current
                Box(Modifier.fillMaxSize().testTag(key.toString()).clickable { coverTaps++ })
            }
            // A full-screen detail with its own bottom action bar, as AppBottomActionBar reports it.
            is Post -> {
                val ownBar = LocalScreenBottomBar.current
                SideEffect { ownBar.value = ACTION_BAR }
                Screen(key)
            }
            else -> Screen(key)
        }
    }

    private fun showShell(state: ShellState, config: ShellConfig = ShellConfig()) {
        rule.setContent {
            AppTheme {
                CompositionLocalProvider(LocalBottomChrome provides bottomChrome) {
                    AppShell(tabs = testTabs, state = state, registry = registry, navigator = AppNavigator(), config = config)
                }
            }
        }
        rule.waitForIdle()
    }

    @Test
    fun `a cover over a tab root takes the touches meant for the bar under it`() {
        val state = shell()
        showShell(state)
        rule.runOnIdle { state.submit(NavCommand.Navigate(Compose)) }
        rule.waitForIdle()

        rule.onNodeWithTag("tab_1", useUnmergedTree = true).performClick()
        rule.waitForIdle()

        assertEquals(0, state.selectedIndex)
        assertEquals(1, coverTaps)
        assertFalse("a presented screen has no tab bar under it", coverSawBar)
    }

    @Test
    fun `Back with a cover over a pushed screen dismisses only the cover`() {
        val state = shell(stacks = listOf(listOf(FeedTab, Post(1)), listOf(ProfileTab)))
        showShell(state)
        rule.runOnIdle { state.submit(NavCommand.Navigate(Compose)) }
        rule.waitForIdle()

        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()

        assertEquals(emptyList<AppNavKey>(), state.presentedKeys)
        assertEquals(listOf(FeedTab, Post(1)), state.current.keys)
        rule.onNodeWithTag(Compose.toString()).assertDoesNotExist()
    }

    @Test
    fun `the bottom chrome is the bar's height, written by the shell`() {
        showShell(shell())

        val bar = rule.onNodeWithTag("tab_0", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue("the bar reserves at least its own height", bottomChrome.value >= with(rule.density) { bar.height.toDp() })
    }

    @Test
    fun `a screen that hides the bar reports its own bottom bar instead`() {
        val state = shell()
        showShell(state, ShellConfig(hideBarOnPush = true))
        val profileTab = rule.onNodeWithTag("tab_1", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.center
        rule.runOnIdle { state.submit(NavCommand.Navigate(Post(1))) }
        rule.waitForIdle()

        assertEquals(ACTION_BAR, bottomChrome.value)
        // Hidden, the bar is out of what accessibility reads and takes no touch where it was.
        rule.onNodeWithTag("tab_0").assertDoesNotExist()
        rule.onRoot().performTouchInput { click(profileTab) }
        rule.runOnIdle { assertEquals(0, state.selectedIndex) }
    }

    @Test
    fun `a full-screen destination hides the bar, even over a pushed screen, and Back brings it back`() {
        val state = shell(stacks = listOf(listOf(FeedTab, Post(1)), listOf(ProfileTab)))
        showShell(state)
        rule.onNodeWithTag("tab_0").assertExists()

        rule.runOnIdle { state.submit(NavCommand.Navigate(Player)) }
        rule.waitForIdle()
        rule.onNodeWithTag("tab_0").assertDoesNotExist()

        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
        rule.onNodeWithTag("tab_0").assertExists()
    }

    @Test
    fun `what covers the bottom edge, in order`() {
        assertEquals(40.dp, bottomChrome(presented = 40.dp, keyboard = 300.dp, screenBar = 72.dp, bar = 96.dp))
        assertEquals(300.dp, bottomChrome(presented = null, keyboard = 300.dp, screenBar = 72.dp, bar = 96.dp))
        assertEquals(72.dp, bottomChrome(presented = null, keyboard = 0.dp, screenBar = 72.dp, bar = 96.dp))
        assertEquals(96.dp, bottomChrome(presented = null, keyboard = 0.dp, screenBar = null, bar = 96.dp))
    }
}
