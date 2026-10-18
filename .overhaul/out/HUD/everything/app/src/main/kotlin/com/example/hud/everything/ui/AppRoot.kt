package com.example.hud.everything.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.example.hud.everything.core.common.navigation.AppNavKey
import com.example.hud.everything.core.common.session.SessionState
import com.example.hud.everything.core.datastore.settings.AppSettings
import com.example.hud.everything.core.designsystem.theme.AppDesignStyle
import com.example.hud.everything.core.designsystem.theme.AppMotionStyle
import com.example.hud.everything.core.designsystem.theme.AppTheme
import com.example.hud.everything.core.designsystem.theme.ThemeMode
import com.example.hud.everything.core.navigation.AppNavigationHost
import com.example.hud.everything.core.navigation.AppNavigator
import com.example.hud.everything.core.navigation.AppShell
import com.example.hud.everything.core.navigation.NavCommand
import com.example.hud.everything.core.navigation.NavKeySerialization
import com.example.hud.everything.core.navigation.NavRegistry
import com.example.hud.everything.core.navigation.ShellTab
import com.example.hud.everything.core.navigation.rememberAppBackStack
import com.example.hud.everything.core.navigation.rememberShellState
import com.example.hud.everything.core.ui.tooling.DevTooling
import com.example.hud.everything.session.SessionCoordinator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import com.example.hud.everything.core.applock.AppLock
import com.example.hud.everything.core.applock.AppLockGate
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.hud.everything.core.network.appstatus.ApiSignals
import com.example.hud.everything.core.ui.appstatus.AppStatusGate
import com.example.hud.everything.core.playstore.PlayStorePrompts
import com.example.hud.everything.core.playstore.review.ReviewPrompt
import com.example.hud.everything.core.playstore.update.AppUpdates

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
    appLock: AppLock,
    apiSignals: ApiSignals,
    appUpdates: AppUpdates,
    reviewPrompt: ReviewPrompt,
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

            ApiStatusGate(apiSignals)

            PlayStorePrompts(updates = appUpdates, review = reviewPrompt)

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
        // Over the inspector too: what the app sent is as private as what it shows.
        AppLockGate(lock = appLock, modifier = Modifier.fillMaxSize(), content = screens)
    }
}

/** Reads the status in a scope of its own: a change recomposes the gate alone, not the root's whole content. */
@Composable
private fun ApiStatusGate(signals: ApiSignals) {
    val status by signals.status.collectAsStateWithLifecycle()
    AppStatusGate(status = status, onRetry = signals::retry)
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
