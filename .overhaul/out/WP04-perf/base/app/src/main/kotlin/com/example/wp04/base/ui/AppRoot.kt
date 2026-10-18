package com.example.wp04.base.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.example.wp04.base.core.datastore.AppSettings
import com.example.wp04.base.core.devtools.DevEnvironment
import com.example.wp04.base.core.devtools.DevToolsOverlay
import com.example.wp04.base.core.devtools.DevToolsLog
import com.example.wp04.base.core.designsystem.theme.AppDesignStyle
import com.example.wp04.base.core.designsystem.theme.AppMotionStyle
import com.example.wp04.base.core.designsystem.theme.AppTheme
import com.example.wp04.base.core.designsystem.theme.ThemeMode
import com.example.wp04.base.core.navigation.AppNavKey
import com.example.wp04.base.core.navigation.AppNavigationHost
import com.example.wp04.base.core.navigation.AppNavigator
import com.example.wp04.base.core.navigation.AppShell
import com.example.wp04.base.core.navigation.NavCommand
import com.example.wp04.base.core.navigation.NavKeySerialization
import com.example.wp04.base.core.navigation.NavRegistry
import com.example.wp04.base.core.navigation.ShellTab
import com.example.wp04.base.core.navigation.rememberAppBackStack
import com.example.wp04.base.core.navigation.rememberShellState
import com.example.wp04.base.session.SessionCoordinator

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
        designStyle = AppDesignStyle.Social,
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
