package io.example.fitcnet.core.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import io.example.fitcnet.core.common.navigation.AppNavKey
import io.example.fitcnet.core.designsystem.theme.AppTheme
import io.example.fitcnet.core.navigation.back.submit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Process death: the tab in front, every stack and what was presented come back, and Back still works. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class RestoreTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `stacks, the selected tab and the presentation stack survive a restore`() {
        val restoration = StateRestorationTester(rule)
        val registry = testRegistry()
        var state: ShellState? = null
        restoration.setContent {
            AppTheme {
                val shell = rememberShellState(tabs = testTabs, registry = registry, serialization = testSerialization, generation = 1)
                state = shell
                AppShell(tabs = testTabs, state = shell, registry = registry, navigator = AppNavigator())
            }
        }
        rule.runOnIdle {
            state!!.submit(NavCommand.Navigate(Post(1)))
            state!!.select(1)
            state!!.submit(NavCommand.Navigate(Compose))
        }
        rule.waitForIdle()
        val before = state

        restoration.emulateSavedInstanceStateRestore()
        rule.waitForIdle()

        val restored = state!!
        assertNotSame("the state was rebuilt from what was saved", before, restored)
        assertEquals(1, restored.selectedIndex)
        assertEquals(listOf(listOf(FeedTab, Post(1)), listOf(ProfileTab)), restored.stacks)
        assertEquals(listOf<AppNavKey>(Compose), restored.presentedKeys)

        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
        assertEquals(emptyList<AppNavKey>(), restored.presentedKeys)
        assertEquals(listOf(listOf(FeedTab, Post(1)), listOf(ProfileTab)), restored.stacks)
    }

    @Test
    fun `reordered tabs each get their own stack back`() {
        val restoration = StateRestorationTester(rule)
        val registry = testRegistry()
        var tabs = testTabs
        var state: ShellState? = null
        restoration.setContent {
            AppTheme {
                val shell = rememberShellState(tabs = tabs, registry = registry, serialization = testSerialization, generation = 1)
                state = shell
            }
        }
        rule.runOnIdle { state!!.submit(NavCommand.Navigate(Post(1))) }
        tabs = testTabs.reversed()

        restoration.emulateSavedInstanceStateRestore()
        rule.waitForIdle()

        assertEquals(listOf(listOf(ProfileTab), listOf(FeedTab, Post(1))), state!!.stacks)
    }
}
