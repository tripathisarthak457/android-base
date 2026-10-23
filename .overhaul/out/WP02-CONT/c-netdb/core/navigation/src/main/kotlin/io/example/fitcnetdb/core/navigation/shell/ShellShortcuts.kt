package io.example.fitcnetdb.core.navigation.shell

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.res.stringResource
import io.example.fitcnetdb.core.navigation.NavigationChrome
import io.example.fitcnetdb.core.navigation.R
import io.example.fitcnetdb.core.navigation.ShellState
import io.example.fitcnetdb.core.navigation.Shortcut
import io.example.fitcnetdb.core.navigation.ShortcutEffect

/**
 * The shell's keyboard shortcuts: Ctrl+1…9 show a tab, Ctrl+[ and Alt+← go back as the Back key
 * does, and Ctrl+Shift+S shows or folds the sidebar where the window has room for one.
 */
@Composable
internal fun ShellShortcuts(labels: List<String>, state: ShellState, onToggleSidebar: (() -> Unit)?) {
    labels.take(TabKeys.size).forEachIndexed { index, label ->
        val shortcut = remember(index, label) { Shortcut(TabKeys[index], label, ctrl = true) }
        ShortcutEffect(shortcut) { if (index != state.selectedIndex || state.moreShown) state.select(index) }
    }
    val back = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    if (back != null) {
        val label = stringResource(R.string.navigation_back)
        val bracket = remember(label) { Shortcut(Key.LeftBracket, label, ctrl = true) }
        val arrow = remember(label) { Shortcut(Key.DirectionLeft, label, alt = true) }
        ShortcutEffect(bracket) { back.onBackPressed() }
        ShortcutEffect(arrow) { back.onBackPressed() }
    }
    if (onToggleSidebar != null) {
        val label = stringResource(
            if (state.chrome == NavigationChrome.Sidebar) R.string.navigation_hide_sidebar else R.string.navigation_show_sidebar,
        )
        val sidebar = remember(label) { Shortcut(Key.S, label, ctrl = true, shift = true) }
        ShortcutEffect(sidebar, onToggleSidebar)
    }
}

private val TabKeys = listOf(Key.One, Key.Two, Key.Three, Key.Four, Key.Five, Key.Six, Key.Seven, Key.Eight, Key.Nine)
