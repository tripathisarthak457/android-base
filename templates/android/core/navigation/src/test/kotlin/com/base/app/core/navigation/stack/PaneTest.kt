package com.base.app.core.navigation.stack

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.base.app.core.common.navigation.AppNavKey
import com.base.app.core.designsystem.foundation.LocalPaneContext
import com.base.app.core.designsystem.foundation.PaneContext
import com.base.app.core.designsystem.foundation.PaneRole
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.navigation.AppBackStack
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
 * sits, and how Back walks back through them before it leaves the list.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK], qualifiers = "w1000dp-h800dp")
class PaneTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val contexts = HashMap<AppNavKey, PaneContext>()

    private val registry = NavRegistry(
        setOf(
            navGraph {
                entry<Inbox>(pane = NavPane.List) { key ->
                    contexts[key] = LocalPaneContext.current
                    Screen(key)
                }
                entry<Mail>(pane = NavPane.Detail) { key ->
                    contexts[key] = LocalPaneContext.current
                    Screen(key)
                }
            },
        ),
    )

    private fun show(stack: AppBackStack) {
        rule.setContent { AppTheme { StackHost(stack = stack, registry = registry, motion = ScreenMotion()) } }
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

    private fun back() {
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
    }
}
