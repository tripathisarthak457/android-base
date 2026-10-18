package com.base.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.base.app.core.common.navigation.AppNavKey
import com.base.app.core.common.session.SessionState
import com.base.app.core.datastore.settings.AppSettings
// <opt:devtools>
import com.base.app.core.devtools.DevTools
import com.base.app.core.devtools.badge.DevToolsOverlay
// </opt:devtools>
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.ThemeMode
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
// <opt:applock>
import com.base.app.core.applock.AppLock
import com.base.app.core.applock.AppLockGate
// </opt:applock>
// <opt:appstatus>
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.base.app.core.network.appstatus.ApiSignals
import com.base.app.core.ui.appstatus.AppStatusGate
// </opt:appstatus>
// <opt:playstore>
import com.base.app.core.playstore.PlayStorePrompts
import com.base.app.core.playstore.review.ReviewPrompt
import com.base.app.core.playstore.update.AppUpdates
// </opt:playstore>

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
    devTools: DevTools,
    // </opt:devtools>
    // <opt:applock>
    appLock: AppLock,
    // </opt:applock>
    // <opt:appstatus>
    apiSignals: ApiSignals,
    // </opt:appstatus>
    // <opt:playstore>
    appUpdates: AppUpdates,
    reviewPrompt: ReviewPrompt,
    // </opt:playstore>
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
                // <opt:devtools>
                ReportCurrentScreen(
                    top = { backStack.current },
                    serialization = serialization,
                    screen = devTools.currentScreen,
                )
                // </opt:devtools>
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
                // <opt:devtools>
                ReportCurrentScreen(
                    top = { shellState.current.current },
                    serialization = serialization,
                    screen = devTools.currentScreen,
                )
                // </opt:devtools>
            }

            // <opt:appstatus>
            ApiStatusGate(apiSignals)
            // </opt:appstatus>

            // <opt:playstore>
            PlayStorePrompts(updates = appUpdates, review = reviewPrompt)
            // </opt:playstore>

            // <opt:devtools>
            // Last in the Box so the badge floats over every screen; a no-op in production builds.
            DevToolsOverlay(devTools)
            // </opt:devtools>
        }
    }

    AppTheme(mode = themeMode, hapticsEnabled = settings.hapticsEnabled) {
        // <opt:applock>
        // Over the inspector too: what the app sent is as private as what it shows.
        AppLockGate(lock = appLock, modifier = Modifier.fillMaxSize(), content = screens)
        // </opt:applock>
        // <opt-else:applock>
        //~ screens()
        // </opt-else:applock>
    }
}

// <opt:appstatus>
/** Reads the status in a scope of its own: a change recomposes the gate alone, not the root's whole content. */
@Composable
private fun ApiStatusGate(signals: ApiSignals) {
    val status by signals.status.collectAsStateWithLifecycle()
    AppStatusGate(status = status, onRetry = signals::retry)
}
// </opt:appstatus>

/** Each end of a session after this point, whoever ended it. */
private fun SessionCoordinator.endedSessions(): Flow<SessionState> =
    state.drop(1).filter { it == SessionState.SignedOut }

/** The stored preference as the design system's own type. */
fun AppSettings.themeMode(): ThemeMode = when (themeMode) {
    AppSettings.THEME_LIGHT -> ThemeMode.Light
    AppSettings.THEME_DARK -> ThemeMode.Dark
    else -> ThemeMode.System
}
