package com.base.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.base.app.core.datastore.AppSettings
// <opt:devtools>
import com.base.app.core.devtools.DevEnvironment
import com.base.app.core.devtools.DevToolsOverlay
import com.base.app.core.devtools.DevToolsLog
// </opt:devtools>
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
    // <opt:devtools>
    devEnvironment: DevEnvironment,
    devToolsLog: DevToolsLog,
    // </opt:devtools>
) {
    val themeMode = settings.themeMode()

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

            // <opt:devtools>
            // Last in the Box so the badge floats over every screen; a no-op in production builds.
            DevToolsOverlay(environment = devEnvironment, log = devToolsLog)
            // </opt:devtools>
        }
    }
}

/** The stored preference as the design system's own type. */
fun AppSettings.themeMode(): ThemeMode = when (themeMode) {
    AppSettings.THEME_LIGHT -> ThemeMode.Light
    AppSettings.THEME_DARK -> ThemeMode.Dark
    else -> ThemeMode.System
}
