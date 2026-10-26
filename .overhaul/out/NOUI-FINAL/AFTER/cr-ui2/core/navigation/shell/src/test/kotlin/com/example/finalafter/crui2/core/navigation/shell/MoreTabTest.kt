package com.example.finalafter.crui2.core.navigation.shell

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.finalafter.crui2.core.common.navigation.AppNavKey
import com.example.finalafter.crui2.core.designsystem.R
import com.example.finalafter.crui2.core.designsystem.icon.AppIcons
import com.example.finalafter.crui2.core.designsystem.theme.AppTheme
import com.example.finalafter.crui2.core.navigation.AppNavigator
import com.example.finalafter.crui2.core.navigation.NavKeySerialization
import com.example.finalafter.crui2.core.navigation.NavRegistry
import com.example.finalafter.crui2.core.navigation.navGraph
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
internal data class Tab(val number: Int) : AppNavKey

/**
 * Six tabs on a phone: the bar shows four and More, whose list opens the other two as tabs of their
 * own; Back walks from such a tab to the list and from the list to the first tab.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK], qualifiers = "w411dp-h891dp")
class MoreTabTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val tabs = List(6) { ShellTab(Tab(it), R.string.designsystem_search, AppIcons.Home) }
    private val graphs = setOf(navGraph { entry<Tab> { Screen(it) } })
    private val registry = NavRegistry(graphs)
    private val serialization = NavKeySerialization(graphs)

    private fun showShell(): ShellState {
        var state: ShellState? = null
        rule.setContent {
            AppTheme {
                val shell = rememberShellState(tabs, registry, serialization, generation = 1)
                state = shell
                AppShell(tabs = tabs, state = shell, registry = registry, navigator = AppNavigator())
            }
        }
        rule.waitForIdle()
        return state!!
    }

    @Test
    fun `tabs past the fourth are listed under More and open as tabs`() {
        val state = showShell()
        assertEquals(NavigationChrome.Bar, state.chrome)
        assertEquals(4, state.overflowFrom)

        rule.onNodeWithTag("tab_4").performClick()
        rule.waitForIdle()
        assertTrue(state.moreShown)
        rule.onNodeWithTag("more_5").performClick()
        rule.waitForIdle()

        assertFalse(state.moreShown)
        assertEquals(5, state.selectedIndex)
        rule.onNodeWithTag(Tab(5).toString()).assertExists()
        // More stays selected in the bar while a tab it lists is in front.
        rule.onNodeWithTag("tab_4").assertIsSelected()
    }

    @Test
    fun `Back from a listed tab returns to the list, and from the list to the first tab`() {
        val state = showShell()
        rule.runOnIdle {
            state.showMore()
            state.select(5)
        }

        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.runOnIdle { assertTrue(state.moreShown) }
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }

        rule.runOnIdle {
            assertFalse(state.moreShown)
            assertEquals(0, state.selectedIndex)
        }
    }

    @Test
    fun `More and the sidebar choice survive a restore`() {
        val restoration = StateRestorationTester(rule)
        var state: ShellState? = null
        restoration.setContent {
            AppTheme {
                val shell = rememberShellState(tabs, registry, serialization, generation = 1)
                state = shell
                AppShell(tabs = tabs, state = shell, registry = registry, navigator = AppNavigator())
            }
        }
        rule.runOnIdle {
            state!!.select(5)
            state!!.showMore()
            state!!.showSidebar(false)
        }

        restoration.emulateSavedInstanceStateRestore()

        rule.runOnIdle {
            assertTrue(state!!.moreShown)
            assertEquals(5, state!!.selectedIndex)
            assertEquals(false, state!!.sidebarOpen)
        }
    }
}
