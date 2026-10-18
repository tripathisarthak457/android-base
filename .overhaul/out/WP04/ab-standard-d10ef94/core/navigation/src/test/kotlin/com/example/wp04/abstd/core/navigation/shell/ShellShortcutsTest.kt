package com.example.wp04.abstd.core.navigation.shell

import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.wp04.abstd.core.designsystem.theme.AppTheme
import com.example.wp04.abstd.core.navigation.AppNavigator
import com.example.wp04.abstd.core.navigation.AppShell
import com.example.wp04.abstd.core.navigation.AppShortcuts
import com.example.wp04.abstd.core.navigation.LocalAppShortcuts
import com.example.wp04.abstd.core.navigation.NavCommand
import com.example.wp04.abstd.core.navigation.Post
import com.example.wp04.abstd.core.navigation.ROBOLECTRIC_SDK
import com.example.wp04.abstd.core.navigation.ShellState
import com.example.wp04.abstd.core.navigation.rememberShellState
import com.example.wp04.abstd.core.navigation.testRegistry
import com.example.wp04.abstd.core.navigation.testSerialization
import com.example.wp04.abstd.core.navigation.testTabs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The keys a keyboard or a Chromebook brings to the shell: Ctrl and a digit for a tab, Ctrl+[ and Alt+← for Back. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK], qualifiers = "w411dp-h891dp")
class ShellShortcutsTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val shortcuts = AppShortcuts()
    private val registry = testRegistry()

    private fun showShell(): ShellState {
        var state: ShellState? = null
        rule.setContent {
            AppTheme {
                CompositionLocalProvider(LocalAppShortcuts provides shortcuts) {
                    val shell = rememberShellState(testTabs, registry, testSerialization, generation = 1)
                    state = shell
                    AppShell(tabs = testTabs, state = shell, registry = registry, navigator = AppNavigator())
                }
            }
        }
        rule.waitForIdle()
        return state!!
    }

    @Test
    fun `Ctrl and a digit show that tab, and a digit past the last tab does nothing`() {
        val state = showShell()

        rule.runOnIdle { assertTrue(shortcuts.handle(key(KeyEvent.KEYCODE_2, KeyEvent.META_CTRL_ON))) }
        rule.runOnIdle { assertEquals(1, state.selectedIndex) }
        rule.runOnIdle { assertTrue(shortcuts.handle(key(KeyEvent.KEYCODE_1, KeyEvent.META_CTRL_ON))) }
        rule.runOnIdle { assertEquals(0, state.selectedIndex) }

        rule.runOnIdle { assertFalse(shortcuts.handle(key(KeyEvent.KEYCODE_3, KeyEvent.META_CTRL_ON))) }
        rule.runOnIdle { assertEquals(0, state.selectedIndex) }
    }

    @Test
    fun `Ctrl+bracket and Alt+left go back as the Back key does`() {
        val state = showShell()
        rule.runOnIdle { state.submit(NavCommand.Navigate(Post(1))) }
        rule.runOnIdle { state.submit(NavCommand.Navigate(Post(2))) }
        rule.waitForIdle()

        rule.runOnIdle { assertTrue(shortcuts.handle(key(KeyEvent.KEYCODE_LEFT_BRACKET, KeyEvent.META_CTRL_ON))) }
        rule.waitForIdle()
        rule.runOnIdle { assertEquals(Post(1), state.current.current) }

        rule.runOnIdle { assertTrue(shortcuts.handle(key(KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.META_ALT_ON))) }
        rule.waitForIdle()
        rule.runOnIdle { assertEquals(1, state.current.size) }
    }

    private fun key(code: Int, meta: Int) = KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, code, 0, meta)
}
