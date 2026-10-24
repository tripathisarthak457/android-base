package com.example.deskstandard.core.navigation

import android.view.KeyEvent
import android.view.KeyboardShortcutGroup
import android.view.KeyboardShortcutInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.lifecycle.compose.LifecycleResumeEffect

/** A key with its modifiers, and what it does in words, as the Shortcuts Helper (Meta+/) lists it. */
@Immutable
class Shortcut(
    val key: Key,
    val label: String,
    val ctrl: Boolean = false,
    val shift: Boolean = false,
    val alt: Boolean = false,
) {
    internal fun matches(event: KeyEvent): Boolean =
        event.keyCode == key.nativeKeyCode && event.isCtrlPressed == ctrl && event.isShiftPressed == shift &&
            event.isAltPressed == alt

    internal val metaState: Int
        get() = (if (ctrl) KeyEvent.META_CTRL_ON else 0) or (if (shift) KeyEvent.META_SHIFT_ON else 0) or
            (if (alt) KeyEvent.META_ALT_ON else 0)
}

/**
 * One window's keyboard shortcuts: the shell's (Ctrl+1…9 for the tabs, Ctrl+[ and Alt+← for Back,
 * Ctrl+Shift+S for the sidebar) and those of the screen in front, which registers its own with
 * [ShortcutEffect]. The activity hands every key press here first, so a shortcut works whatever has
 * focus, and lists them for the Shortcuts Helper.
 */
@Stable
class AppShortcuts {

    private class Registration(val shortcut: Shortcut, val action: () -> Unit)

    private val registrations = ArrayList<Registration>()

    /** Runs the newest shortcut [event] matches and returns true, or returns false. */
    fun handle(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN || event.repeatCount > 0) return false
        val registration = registrations.lastOrNull { it.shortcut.matches(event) } ?: return false
        registration.action()
        return true
    }

    /** What the window answers to now, as one group for `Activity.onProvideKeyboardShortcuts`. */
    fun group(title: CharSequence): KeyboardShortcutGroup = KeyboardShortcutGroup(
        title,
        registrations.distinctBy { it.shortcut.key to it.shortcut.metaState }.map { registration ->
            KeyboardShortcutInfo(registration.shortcut.label, registration.shortcut.key.nativeKeyCode, registration.shortcut.metaState)
        },
    )

    internal fun register(shortcut: Shortcut, action: () -> Unit): () -> Unit {
        val registration = Registration(shortcut, action)
        registrations += registration
        return { registrations.remove(registration) }
    }
}

/** The window's shortcuts; null where nothing provides them, such as a preview. */
val LocalAppShortcuts = staticCompositionLocalOf<AppShortcuts?> { null }

/**
 * Answers [shortcut] while the screen that calls this is the one in front. A screen under another
 * one, or in a tab that is not selected, stays composed but is not resumed, so its shortcuts are
 * off until it is shown again.
 */
@Composable
fun ShortcutEffect(shortcut: Shortcut, action: () -> Unit) {
    val shortcuts = LocalAppShortcuts.current ?: return
    val latest = rememberUpdatedState(action)
    LifecycleResumeEffect(shortcuts, shortcut) {
        val unregister = shortcuts.register(shortcut) { latest.value() }
        onPauseOrDispose { unregister() }
    }
}
