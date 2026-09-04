package com.base.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.base.app.core.datastore.AppSettings
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.ThemeMode
import com.base.app.core.navigation.AppNavKey
import com.base.app.core.navigation.AppNavigationHost
import com.base.app.core.navigation.AppNavigator
import com.base.app.core.navigation.AppShell
import com.base.app.core.navigation.NavCommand
import com.base.app.core.navigation.NavKeySerialization
import com.base.app.core.navigation.NavRegistry
import com.base.app.core.navigation.ShellTab
import com.base.app.core.navigation.rememberAppBackStack
import com.base.app.core.navigation.rememberShellState
import com.base.app.session.SessionCoordinator

/**
 * Everything above the navigation host: the theme, the back stack, and the two app-wide reactions
 * that have to outlive any single screen.
 *
 * ## One shape or the other, decided by [tabs]
 *
 * An empty list gives a single back stack — the right shape for a wizard, a kiosk, or an app
 * whose entry point is a sign-in screen. A non-empty one gives the tabbed shell, where each tab
 * keeps its own stack so switching away and back does not lose the user's place.
 *
 * The decision is a parameter rather than two entry points because sign-in and the signed-in app
 * are usually both of these at different moments, and swapping between them should not mean
 * swapping which composable is at the root.
 *
 * ## The theme is state, not a constant
 *
 * Read from the settings store as a flow, so changing it in settings repaints immediately rather
 * than on next launch. Haptics come from the same place: one boolean here silences every control
 * in the app, and no component owns the decision.
 *
 * ## Sign-out resets here
 *
 * Not in whatever screen called `signOut()`: that screen is being destroyed. This collector lives
 * as long as the app does.
 */
@Composable
fun AppRoot(
    startKey: AppNavKey,
    navigator: AppNavigator,
    registry: NavRegistry,
    serialization: NavKeySerialization,
    sessionCoordinator: SessionCoordinator,
    settings: AppSettings,
    tabs: List<ShellTab> = emptyList(),
    signInKey: AppNavKey = startKey,
    onExitRequested: () -> Unit = {},
) {
    val themeMode = when (settings.themeMode) {
        AppSettings.THEME_LIGHT -> ThemeMode.Light
        AppSettings.THEME_DARK -> ThemeMode.Dark
        else -> ThemeMode.System
    }

    AppTheme(mode = themeMode, hapticsEnabled = settings.hapticsEnabled) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (tabs.isEmpty()) {
                val backStack = rememberAppBackStack(startKey = startKey, serialization = serialization)

                LaunchedEffect(sessionCoordinator, backStack) {
                    sessionCoordinator.signedOut.collect {
                        backStack.apply(NavCommand.ResetTo(signInKey))
                    }
                }

                AppNavigationHost(
                    backStack = backStack,
                    registry = registry,
                    navigator = navigator,
                    onExitRequested = onExitRequested,
                )
            } else {
                val shellState = rememberShellState(tabs = tabs, serialization = serialization)

                LaunchedEffect(sessionCoordinator, shellState) {
                    sessionCoordinator.signedOut.collect {
                        // Every tab, not just the visible one: the next person to sign in on this
                        // device must not find the previous one's screens behind a tab.
                        shellState.resetAll(tabs.map { it.key })
                        shellState.current.apply(NavCommand.ResetTo(signInKey))
                    }
                }

                AppShell(
                    tabs = tabs,
                    state = shellState,
                    registry = registry,
                    navigator = navigator,
                    onExitRequested = onExitRequested,
                )
            }
        }
    }
}
