package com.example.standard.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.example.standard.core.common.navigation.AppNavKey
import com.example.standard.core.datastore.AppSettings
import com.example.standard.core.designsystem.theme.AppDesignStyle
import com.example.standard.core.designsystem.theme.AppMotionStyle
import com.example.standard.core.designsystem.theme.AppTheme
import com.example.standard.core.designsystem.theme.ThemeMode
import com.example.standard.core.devtools.DevEnvironment
import com.example.standard.core.devtools.DevToolsLog
import com.example.standard.core.devtools.DevToolsOverlay
import com.example.standard.core.navigation.AppNavigationHost
import com.example.standard.core.navigation.AppNavigator
import com.example.standard.core.navigation.AppShell
import com.example.standard.core.navigation.NavCommand
import com.example.standard.core.navigation.NavKeySerialization
import com.example.standard.core.navigation.NavRegistry
import com.example.standard.core.navigation.ShellTab
import com.example.standard.core.navigation.rememberAppBackStack
import com.example.standard.core.navigation.rememberShellState
import com.example.standard.session.SessionCoordinator

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
    devEnvironment: DevEnvironment,
    devToolsLog: DevToolsLog,
) {
    val themeMode = settings.themeMode()

    AppTheme(
        mode = themeMode,
        motionStyle = AppMotionStyle.Standard,
        designStyle = AppDesignStyle.Utility,
        hapticsEnabled = settings.hapticsEnabled,
    ) {
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

            // Last in the Box so the badge floats over every screen; a no-op in production builds.
            DevToolsOverlay(environment = devEnvironment, log = devToolsLog)
        }
    }
}

/** The stored preference as the design system's own type. */
fun AppSettings.themeMode(): ThemeMode = when (themeMode) {
    AppSettings.THEME_LIGHT -> ThemeMode.Light
    AppSettings.THEME_DARK -> ThemeMode.Dark
    else -> ThemeMode.System
}
