package com.base.app.core.navigation.stack

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.WindowSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.base.app.core.common.navigation.AppNavKey
import com.base.app.core.designsystem.foundation.layout.LocalPaneContext
import com.base.app.core.designsystem.foundation.layout.PaneContext
import com.base.app.core.designsystem.foundation.layout.PaneRole
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.navigation.AppBackStack
import com.base.app.core.navigation.NavCommand
import com.base.app.core.navigation.NavPane
import com.base.app.core.navigation.NavRegistry
import com.base.app.core.navigation.ROBOLECTRIC_SDK
import com.base.app.core.navigation.Screen
import com.base.app.core.navigation.navGraph
import com.base.app.core.navigation.transition.ScreenMotion
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Serializable
internal data object Inbox : AppNavKey

@Serializable
internal data class Mail(val id: Int) : AppNavKey

/**
 * A list and its details on a window with room for two panes: what each pane is told about where it
 * sits, how Back walks back through them before it leaves the list, and how a fold or an unfold
 * moves them between a stack and side by side.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK], qualifiers = "w1000dp-h800dp")
class PaneTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val contexts = HashMap<AppNavKey, PaneContext>()
    private var inbox: LazyListState? = null
    private var windowSize by mutableStateOf(DpSize(1000.dp, 800.dp))

    private val registry = NavRegistry(
        setOf(
            navGraph {
                entry<Inbox>(pane = NavPane.List) { key ->
                    contexts[key] = LocalPaneContext.current
                    val list = rememberLazyListState()
                    inbox = list
                    LazyColumn(Modifier.fillMaxSize().testTag(key.toString()), state = list) {
                        items(count = 100, key = { it }) { BasicText("Mail $it", Modifier.height(48.dp)) }
                    }
                }
                entry<Mail>(pane = NavPane.Detail) { key ->
                    contexts[key] = LocalPaneContext.current
                    Screen(key)
                }
            },
        ),
    )

    private fun show(stack: AppBackStack) {
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.WindowSize(windowSize)) {
                AppTheme { StackHost(stack = stack, registry = registry, motion = ScreenMotion()) }
            }
        }
        rule.waitForIdle()
    }

    @Test
    fun `each pane knows its role, and the list knows which detail is open beside it`() {
        show(AppBackStack(mutableStateListOf(Inbox, Mail(1), Mail(2))))

        assertEquals(PaneContext(PaneRole.List, beside = true, selectedKey = Mail(2)), contexts[Inbox])
        assertEquals(PaneContext(PaneRole.Detail, beside = true, detailDepth = 2), contexts[Mail(2)])
        assertFalse("a detail under the one shown is not beside the list", contexts.getValue(Mail(1)).beside)
    }

    @Test
    fun `Back pops the details first, then shows the empty pane, then leaves the list to the shell`() {
        val stack = AppBackStack(mutableStateListOf(Inbox, Mail(1), Mail(2)))
        show(stack)

        back()
        assertEquals(listOf<AppNavKey>(Inbox, Mail(1)), stack.keys)
        rule.onNodeWithTag(Inbox.toString(), useUnmergedTree = true).assertExists()
        assertEquals(Mail(1), contexts.getValue(Inbox).selectedKey)

        back()
        assertEquals(listOf<AppNavKey>(Inbox), stack.keys)
        assertEquals(null, contexts.getValue(Inbox).selectedKey)
        assertTrue("the empty detail pane keeps the list beside something", contexts.getValue(Inbox).beside)

        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        assertTrue("Back at the list is the shell's or the system's", rule.activity.isFinishing)
    }

    @Test
    fun `a fold stacks the detail over its list, and an unfold puts them side by side again`() {
        windowSize = DpSize(1280.dp, 800.dp)
        val stack = AppBackStack(mutableStateListOf(Inbox))
        show(stack)
        rule.onNodeWithTag(Inbox.toString()).performScrollToIndex(30)
        rule.runOnIdle { stack.apply(NavCommand.Navigate(Mail(30))) }
        rule.waitForIdle()
        assertEquals(Mail(30), contexts.getValue(Inbox).selectedKey)

        resize(DpSize(411.dp, 891.dp))
        rule.onNodeWithTag(Mail(30).toString()).assertIsDisplayed()
        rule.onNodeWithTag(Inbox.toString()).assertDoesNotExist()
        assertFalse(contexts.getValue(Mail(30)).beside)

        resize(DpSize(1280.dp, 800.dp))
        rule.onNodeWithTag(Inbox.toString()).assertIsDisplayed()
        rule.onNodeWithTag(Mail(30).toString()).assertIsDisplayed()
        assertEquals(Mail(30), contexts.getValue(Inbox).selectedKey)

        resize(DpSize(411.dp, 891.dp))
        back()
        assertEquals(listOf<AppNavKey>(Inbox), stack.keys)
        rule.onNodeWithTag(Inbox.toString()).assertIsDisplayed()
        assertEquals("the list kept its place", 30, inbox!!.firstVisibleItemIndex)
    }

    private fun resize(size: DpSize) {
        windowSize = size
        rule.waitForIdle()
    }

    private fun back() {
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
    }
}
