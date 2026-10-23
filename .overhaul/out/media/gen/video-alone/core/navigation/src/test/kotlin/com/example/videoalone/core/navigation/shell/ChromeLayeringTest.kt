package com.example.videoalone.core.navigation.shell

import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.videoalone.core.common.navigation.AppNavKey
import com.example.videoalone.core.designsystem.foundation.layout.ChromeInset
import com.example.videoalone.core.designsystem.foundation.layout.LocalBottomChrome
import com.example.videoalone.core.designsystem.foundation.layout.LocalScreenChrome
import com.example.videoalone.core.designsystem.foundation.layout.LocalTabBarVisible
import com.example.videoalone.core.designsystem.foundation.layout.LocalWindowTopChrome
import com.example.videoalone.core.designsystem.foundation.layout.ReportScrollEdge
import com.example.videoalone.core.designsystem.foundation.layout.chromeContentPadding
import com.example.videoalone.core.designsystem.foundation.scroll.AppScrollDefaults
import com.example.videoalone.core.designsystem.theme.AppTheme
import com.example.videoalone.core.navigation.AppNavigator
import com.example.videoalone.core.navigation.AppShell
import com.example.videoalone.core.navigation.Compose
import com.example.videoalone.core.navigation.FeedTab
import com.example.videoalone.core.navigation.NavCommand
import com.example.videoalone.core.navigation.NavRegistry
import com.example.videoalone.core.navigation.Player
import com.example.videoalone.core.navigation.Post
import com.example.videoalone.core.navigation.ProfileTab
import com.example.videoalone.core.navigation.ROBOLECTRIC_SDK
import com.example.videoalone.core.navigation.Screen
import com.example.videoalone.core.navigation.ShellConfig
import com.example.videoalone.core.navigation.ShellState
import com.example.videoalone.core.navigation.back.submit
import com.example.videoalone.core.navigation.shell
import com.example.videoalone.core.navigation.testRegistry
import com.example.videoalone.core.navigation.testTabs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private val ACTION_BAR = 72.dp
private val TOP_BAR = 104.dp
private const val FEED_LIST = "feed_list"

/** Taller than the bar with its margins, so the keyboard covers all of it. */
private const val KEYBOARD_PX = 900

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
    private val windowTopChrome = ChromeInset()

    private val registry = testRegistry { key ->
        when (key) {
            Compose -> {
                coverSawBar = LocalTabBarVisible.current
                Box(Modifier.fillMaxSize().testTag(key.toString()).clickable { coverTaps++ })
            }
            // A full-screen detail with its own bottom action bar and a top bar, as the scaffold
            // reports them.
            is Post -> {
                val screen = LocalScreenChrome.current
                SideEffect {
                    screen.bottomBar = ACTION_BAR
                    screen.topBar = TOP_BAR
                }
                Screen(key)
            }
            else -> Screen(key)
        }
    }

    private fun showShell(state: ShellState, config: ShellConfig = ShellConfig(), registry: NavRegistry = this.registry) {
        rule.setContent {
            AppTheme {
                CompositionLocalProvider(LocalBottomChrome provides bottomChrome, LocalWindowTopChrome provides windowTopChrome) {
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
    fun `a root list scrolls under the floating bar, and its last row comes to rest above it`() {
        val rows = testRegistry { key ->
            if (key == FeedTab) {
                LazyColumn(
                    Modifier.fillMaxSize().testTag(FEED_LIST),
                    contentPadding = chromeContentPadding(),
                    flingBehavior = AppScrollDefaults.flingBehavior(),
                ) {
                    items(count = 40, key = { it }) { BasicText("Row $it", Modifier.fillMaxWidth().height(56.dp).testTag("row_$it")) }
                }
            } else {
                Screen(key)
            }
        }
        showShell(shell(), registry = rows)
        val bar = bounds("tab_bar")

        assertTrue("the list runs on under the bar", bounds(FEED_LIST).bottom >= bar.bottom)

        rule.onNodeWithTag(FEED_LIST).performScrollToIndex(39)
        assertTrue("the last row ends above the bar", bounds("row_39").bottom <= bar.top)
    }

    @Test
    fun `a root list reports whether content runs on under the bar, which clears once it rests on its end`() {
        val rows = testRegistry { key ->
            if (key == FeedTab) {
                val list = rememberLazyListState()
                ReportScrollEdge(list)
                LazyColumn(
                    Modifier.fillMaxSize().testTag(FEED_LIST),
                    state = list,
                    contentPadding = chromeContentPadding(),
                    flingBehavior = AppScrollDefaults.flingBehavior(),
                ) {
                    items(count = 40, key = { it }) { BasicText("Row $it", Modifier.fillMaxWidth().height(56.dp)) }
                }
            } else {
                Screen(key)
            }
        }
        val state = shell()
        showShell(state, registry = rows)
        rule.runOnIdle { assertEquals(true, state.tabFront?.contentBelow) }

        rule.onNodeWithTag(FEED_LIST).performScrollToIndex(39)
        rule.runOnIdle { assertEquals(false, state.tabFront?.contentBelow) }

        // The Profile root reports nothing, so its bar keeps the surface that hides what passes under it.
        rule.runOnIdle { state.select(1) }
        rule.runOnIdle { assertEquals(true, state.tabFront?.contentBelow) }
    }

    @Test
    fun `the window's top chrome is the top bar of the settled screen in front`() {
        val state = shell()
        showShell(state)
        assertEquals(0.dp, windowTopChrome.value)

        rule.runOnIdle { state.submit(NavCommand.Navigate(Post(1))) }
        rule.waitForIdle()
        assertEquals(TOP_BAR, windowTopChrome.value)

        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
        assertEquals(0.dp, windowTopChrome.value)
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
    fun `a screen in the stack twice keeps its bottom bar when the second one is popped`() {
        val state = shell()
        showShell(state, ShellConfig(hideBarOnPush = true))
        listOf(Post(1), Post(2), Post(1)).forEach { key ->
            rule.runOnIdle { state.submit(NavCommand.Navigate(key)) }
            rule.waitForIdle()
        }

        repeat(2) {
            rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
            rule.waitForIdle()
        }

        assertEquals(listOf(FeedTab, Post(1)), state.current.keys)
        assertEquals(ACTION_BAR, bottomChrome.value)
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
    fun `the keyboard sinks the bar under it and becomes the bottom chrome`() {
        showShell(shell())
        val before = bounds("tab_0")

        rule.runOnUiThread {
            val content = rule.activity.findViewById<ViewGroup>(android.R.id.content)
            val composeView = (content.getChildAt(0) as ViewGroup).getChildAt(0)
            val insets = WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, KEYBOARD_PX))
                .setVisible(WindowInsetsCompat.Type.ime(), true)
                .build()
            ViewCompat.dispatchApplyWindowInsets(composeView, insets)
        }
        rule.waitForIdle()

        assertTrue("the bar has gone down under the keyboard", bounds("tab_0").top >= before.bottom)
        assertEquals(with(rule.density) { KEYBOARD_PX.toDp() }, bottomChrome.value)
    }

    @Test
    fun `what covers the bottom edge, in order`() {
        assertEquals(40.dp, bottomChrome(presented = 40.dp, keyboard = 300.dp, screenBar = 72.dp, bar = 96.dp))
        assertEquals(300.dp, bottomChrome(presented = null, keyboard = 300.dp, screenBar = 72.dp, bar = 96.dp))
        assertEquals(72.dp, bottomChrome(presented = null, keyboard = 0.dp, screenBar = 72.dp, bar = 96.dp))
        assertEquals(96.dp, bottomChrome(presented = null, keyboard = 0.dp, screenBar = null, bar = 96.dp))
    }

    private fun bounds(tag: String) = rule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
}
