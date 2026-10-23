package com.example.none.core.navigation

import android.view.KeyEvent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class ShortcutsTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `a key press runs the newest shortcut it matches, once`() {
        val shortcuts = AppShortcuts()
        val ran = mutableListOf<String>()
        shortcuts.register(Shortcut(Key.F, "Find", ctrl = true)) { ran += "shell" }
        shortcuts.register(Shortcut(Key.F, "Find", ctrl = true)) { ran += "screen" }

        assertTrue(shortcuts.handle(key(KeyEvent.KEYCODE_F, KeyEvent.META_CTRL_ON)))
        assertFalse(shortcuts.handle(key(KeyEvent.KEYCODE_F, KeyEvent.META_CTRL_ON, repeat = 1)))
        assertFalse(shortcuts.handle(key(KeyEvent.KEYCODE_F, KeyEvent.META_CTRL_ON or KeyEvent.META_SHIFT_ON)))
        assertFalse(shortcuts.handle(key(KeyEvent.KEYCODE_F, 0)))
        assertFalse(shortcuts.handle(key(KeyEvent.KEYCODE_F, KeyEvent.META_CTRL_ON, action = KeyEvent.ACTION_UP)))

        assertEquals(listOf("screen"), ran)
        assertEquals(1, shortcuts.group("App").items.size)
    }

    @Test
    fun `a screen's shortcut answers only while the screen is resumed`() {
        val shortcuts = AppShortcuts()
        val owner = object : LifecycleOwner {
            override val lifecycle = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.STARTED }
        }
        var found = 0
        rule.setContent {
            CompositionLocalProvider(LocalAppShortcuts provides shortcuts, LocalLifecycleOwner provides owner) {
                ShortcutEffect(Shortcut(Key.F, "Find", ctrl = true)) { found++ }
            }
        }
        val press = key(KeyEvent.KEYCODE_F, KeyEvent.META_CTRL_ON)

        rule.runOnIdle { assertFalse(shortcuts.handle(press)) }
        rule.runOnIdle { owner.lifecycle.currentState = Lifecycle.State.RESUMED }
        rule.runOnIdle { assertTrue(shortcuts.handle(press)) }
        rule.runOnIdle { owner.lifecycle.currentState = Lifecycle.State.STARTED }
        rule.runOnIdle { assertFalse(shortcuts.handle(press)) }

        assertEquals(1, found)
    }

    private fun key(code: Int, meta: Int, repeat: Int = 0, action: Int = KeyEvent.ACTION_DOWN) =
        KeyEvent(0L, 0L, action, code, repeat, meta)
}
