package io.example.fitclocal.core.navigation.shell

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.WindowSize
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import io.example.fitclocal.core.common.navigation.AppNavKey
import io.example.fitclocal.core.designsystem.theme.AppTheme
import io.example.fitclocal.core.navigation.AppNavigator
import io.example.fitclocal.core.navigation.AppShell
import io.example.fitclocal.core.navigation.NavCommand
import io.example.fitclocal.core.navigation.NavigationChrome
import io.example.fitclocal.core.navigation.Post
import io.example.fitclocal.core.navigation.ROBOLECTRIC_SDK
import io.example.fitclocal.core.navigation.Screen
import io.example.fitclocal.core.navigation.ShellState
import io.example.fitclocal.core.navigation.back.submit
import io.example.fitclocal.core.navigation.rememberShellState
import io.example.fitclocal.core.navigation.testRegistry
import io.example.fitclocal.core.navigation.testSerialization
import io.example.fitclocal.core.navigation.testTabs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A window walked through every width class, as a fold, a rotation or a desktop resize does: the
 * chrome follows the class, and every tab, stack and composed screen stays what it was.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK], qualifiers = "w1920dp-h1080dp")
class ResizeContinuityTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `tabs, stacks and screens survive every class flip`() {
        var size by mutableStateOf(DpSize(411.dp, 891.dp))
        // How many times each screen was built: a second build is a screen that lost its state.
        val builds = mutableMapOf<AppNavKey, Int>()
        val registry = testRegistry { key ->
            remember { builds.merge(key, 1, Int::plus) }
            Screen(key)
        }
        lateinit var state: ShellState
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.WindowSize(size)) {
                AppTheme {
                    val shell = rememberShellState(testTabs, registry, testSerialization, generation = 1)
                    state = shell
                    AppShell(tabs = testTabs, state = shell, registry = registry, navigator = AppNavigator())
                }
            }
        }
        rule.runOnIdle {
            state.select(1)
            state.submit(NavCommand.Navigate(Post(7)))
        }
        rule.waitForIdle()
        val stacks = state.stacks

        val walk = listOf(
            DpSize(891.dp, 900.dp) to NavigationChrome.Rail,
            DpSize(1280.dp, 900.dp) to NavigationChrome.Sidebar,
            DpSize(1920.dp, 1080.dp) to NavigationChrome.Sidebar,
            DpSize(673.dp, 900.dp) to NavigationChrome.Rail,
            DpSize(411.dp, 891.dp) to NavigationChrome.Bar,
        )
        for ((next, chrome) in walk) {
            size = next
            rule.waitForIdle()
            assertEquals("$next", chrome, state.chrome)
            rule.onNodeWithTag(chrome.tag).assertExists()
            assertEquals("$next", 1, state.selectedIndex)
            assertEquals("$next", stacks, state.stacks)
            assertFalse("$next", state.current.gate.moving)
            assertEquals("$next", setOf(1), builds.values.toSet())
        }
    }

    private val NavigationChrome.tag: String
        get() = when (this) {
            NavigationChrome.Bar -> "tab_bar"
            NavigationChrome.Rail -> "rail"
            NavigationChrome.Sidebar -> "sidebar"
        }
}
