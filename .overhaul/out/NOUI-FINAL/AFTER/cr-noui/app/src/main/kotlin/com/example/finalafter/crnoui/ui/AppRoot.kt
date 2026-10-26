package com.example.finalafter.crnoui.ui

import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.finalafter.crnoui.core.applock.AppLock
import com.example.finalafter.crnoui.core.applock.AppLockGate
import com.example.finalafter.crnoui.core.datastore.settings.AppSettings
import com.example.finalafter.crnoui.core.designsystem.theme.AppTheme
import com.example.finalafter.crnoui.core.designsystem.theme.ThemeMode
import com.example.finalafter.crnoui.core.navigation.AppNavigationHost
import com.example.finalafter.crnoui.core.navigation.AppNavigator
import com.example.finalafter.crnoui.core.navigation.AppShortcuts
import com.example.finalafter.crnoui.core.navigation.LocalAppShortcuts
import com.example.finalafter.crnoui.core.navigation.NavKeySerialization
import com.example.finalafter.crnoui.core.navigation.NavRegistry
import com.example.finalafter.crnoui.core.navigation.OpenDeepLinks
import com.example.finalafter.crnoui.core.navigation.PendingDeepLink
import com.example.finalafter.crnoui.core.navigation.rememberAppBackStack
import com.example.finalafter.crnoui.core.network.appstatus.ApiSignals
import com.example.finalafter.crnoui.core.playstore.PlayStorePrompts
import com.example.finalafter.crnoui.core.playstore.review.ReviewPrompt
import com.example.finalafter.crnoui.core.playstore.update.AppUpdates
import com.example.finalafter.crnoui.core.ui.appstatus.AppStatusGate
import com.example.finalafter.crnoui.core.ui.tooling.DevTooling
import com.example.finalafter.crnoui.startup.Startup

/**
 * Everything above the navigation host: the theme, the system bars, and the one back stack the app
 * opens on, at [AppDestinations.start].
 *
 * The stack is keyed on who the app is for (first launch, signed out, signed in, and each new
 * session), so onboarding finishing, a sign-in and a sign-out each start a fresh stack, and nothing
 * from one session is left behind for the next. Which screen each of them opens on is
 * [AppDestinations]' choice: today every one opens on the start screen.
 */
@Composable
fun AppRoot(
    startup: Startup,
    navigator: AppNavigator,
    registry: NavRegistry,
    serialization: NavKeySerialization,
    pendingDeepLink: PendingDeepLink,
    shortcuts: AppShortcuts,
    devTooling: DevTooling?,
    appLock: AppLock,
    apiSignals: ApiSignals,
    appUpdates: AppUpdates,
    reviewPrompt: ReviewPrompt,
) {
    val settings = startup.settings
    val themeMode = settings.themeMode()

    val screens: @Composable () -> Unit = {
        // Test tags become resource ids, which is how benchmarks and UI tests find the screens.
        Box(modifier = Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }) {
            CompositionLocalProvider(LocalAppShortcuts provides shortcuts) {
                key(startup.identity) {
                    AppStack(
                        navigator = navigator,
                        registry = registry,
                        serialization = serialization,
                        pendingDeepLink = pendingDeepLink,
                        devTooling = devTooling,
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
        hapticsEnabled =
            AppTheme.defaults.hapticsEnabled && settings.hapticsEnabled,
    ) {
        SystemBarIcons(dark = !AppTheme.colors.isLight)
        // Over the inspector too: what the app sent is as private as what it shows.
        AppLockGate(lock = appLock, modifier = Modifier.fillMaxSize(), content = screens)
    }
}

@Composable
private fun AppStack(
    navigator: AppNavigator,
    registry: NavRegistry,
    serialization: NavKeySerialization,
    pendingDeepLink: PendingDeepLink,
    devTooling: DevTooling?,
) {
    val backStack =
        rememberAppBackStack(startKey = AppDestinations.start, serialization = serialization)
    AppNavigationHost(backStack = backStack, registry = registry, navigator = navigator)
    OpenDeepLinks(stack = backStack, pending = pendingDeepLink)
    if (devTooling != null) {
        ReportCurrentScreen(
            top = { backStack.current },
            serialization = serialization,
            tooling = devTooling,
        )
    }
}

/**
 * Status and navigation bar icons follow the app's theme rather than the system's: an app set to
 * Dark on a light phone would otherwise draw dark icons on a dark screen.
 */
@Composable
private fun SystemBarIcons(dark: Boolean) {
    val activity = LocalActivity.current as? ComponentActivity ?: return
    // Before the frame is laid out and drawn, so no frame shows the previous theme's icons.
    SideEffect(activity, dark) {
        val style = if (dark) {
            SystemBarStyle.dark(Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        }
        activity.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }
}

/** The stored preference as the design system's own type. */
fun AppSettings.themeMode(): ThemeMode = when (themeMode) {
    AppSettings.THEME_LIGHT -> ThemeMode.Light
    AppSettings.THEME_DARK -> ThemeMode.Dark
    else -> ThemeMode.System
}

/**
 * Reads the status in a scope of its own: a change recomposes the gate alone, not the root's whole
 * content.
 */
@Composable
private fun ApiStatusGate(signals: ApiSignals) {
    val status by signals.status.collectAsStateWithLifecycle()
    AppStatusGate(status = status, onRetry = signals::retry)
}
