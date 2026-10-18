package com.example.hud.standard.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.example.hud.standard.core.common.navigation.AppNavKey
import com.example.hud.standard.core.common.session.SessionState
import com.example.hud.standard.core.datastore.settings.AppSettings
import com.example.hud.standard.core.designsystem.theme.AppDesignStyle
import com.example.hud.standard.core.designsystem.theme.AppMotionStyle
import com.example.hud.standard.core.designsystem.theme.AppTheme
import com.example.hud.standard.core.designsystem.theme.ThemeMode
import com.example.hud.standard.core.navigation.AppNavigationHost
import com.example.hud.standard.core.navigation.AppNavigator
import com.example.hud.standard.core.navigation.AppShell
import com.example.hud.standard.core.navigation.NavCommand
import com.example.hud.standard.core.navigation.NavKeySerialization
import com.example.hud.standard.core.navigation.NavRegistry
import com.example.hud.standard.core.navigation.ShellTab
import com.example.hud.standard.core.navigation.rememberAppBackStack
import com.example.hud.standard.core.navigation.rememberShellState
import com.example.hud.standard.core.ui.tooling.DevTooling
import com.example.hud.standard.session.SessionCoordinator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter

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
    devTooling: DevTooling?,
) {
    val themeMode = settings.themeMode()

    val screens: @Composable () -> Unit = {
        Box(modifier = Modifier.fillMaxSize()) {
            if (tabs.isEmpty()) {
                val backStack = rememberAppBackStack(startKey = startKey, serialization = serialization)

                LaunchedEffect(sessionCoordinator, backStack) {
                    sessionCoordinator.endedSessions().collect {
                        backStack.apply(NavCommand.ResetTo(signInKey))
                    }
                }

                AppNavigationHost(
                    backStack = backStack,
                    registry = registry,
                    navigator = navigator,
                    onExitRequested = onExitRequested,
                )
                if (devTooling != null) {
                    ReportCurrentScreen(
                        top = { backStack.current },
                        serialization = serialization,
                        tooling = devTooling,
                    )
                }
            } else {
                val shellState = rememberShellState(tabs = tabs, serialization = serialization)

                LaunchedEffect(sessionCoordinator, shellState) {
                    sessionCoordinator.endedSessions().collect {
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
                if (devTooling != null) {
                    ReportCurrentScreen(
                        top = { shellState.current.current },
                        serialization = serialization,
                        tooling = devTooling,
                    )
                }
            }

            // Last in the Box so the badge floats over every screen.
            devTooling?.Overlay()
        }
    }

    AppTheme(
        mode = themeMode,
        motionStyle = AppMotionStyle.Standard,
        designStyle = AppDesignStyle.Utility,
        hapticsEnabled = settings.hapticsEnabled,
    ) {
        screens()
    }
}

/** Each end of a session after this point, whoever ended it. */
private fun SessionCoordinator.endedSessions(): Flow<SessionState> =
    state.drop(1).filter { it == SessionState.SignedOut }

/** The stored preference as the design system's own type. */
fun AppSettings.themeMode(): ThemeMode = when (themeMode) {
    AppSettings.THEME_LIGHT -> ThemeMode.Light
    AppSettings.THEME_DARK -> ThemeMode.Dark
    else -> ThemeMode.System
}
