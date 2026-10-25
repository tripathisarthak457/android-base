package com.base.app.ui

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
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.base.app.core.datastore.settings.AppSettings
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.ThemeMode
import com.base.app.core.navigation.AppNavigationHost
import com.base.app.core.navigation.AppNavigator
import com.base.app.core.navigation.AppShortcuts
import com.base.app.core.navigation.LocalAppShortcuts
import com.base.app.core.navigation.NavKeySerialization
import com.base.app.core.navigation.NavRegistry
import com.base.app.core.navigation.OpenDeepLinks
import com.base.app.core.navigation.PendingDeepLink
import com.base.app.core.navigation.rememberAppBackStack
// <opt:devtools>
import com.base.app.core.ui.tooling.DevTooling
// </opt:devtools>
import com.base.app.startup.Startup
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
 * Everything above the navigation host: the theme, the system bars, and the one back stack the app
 * opens on, at [AppDestinations.start].
 *
 * The stack is keyed on who the app is for (first launch, signed out, signed in, and each new
 * session), so onboarding finishing, a sign-in and a sign-out each start a fresh stack, and nothing
 * from one session is left behind for the next. Which screen each of them opens on is
 * [AppDestinations]' choice: today every one opens on the start screen.
 */
@Composable
internal fun AppRoot(
    startup: Startup,
    navigator: AppNavigator,
    registry: NavRegistry,
    serialization: NavKeySerialization,
    pendingDeepLink: PendingDeepLink,
    shortcuts: AppShortcuts,
    // <opt:devtools>
    devTooling: DevTooling?,
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
                        // <opt:devtools>
                        devTooling = devTooling,
                        // </opt:devtools>
                    )
                }
            }

            // <opt:appstatus>
            ApiStatusGate(apiSignals)
            // </opt:appstatus>

            // <opt:playstore>
            PlayStorePrompts(updates = appUpdates, review = reviewPrompt)
            // </opt:playstore>

            // <opt:devtools>
            // Last in the Box so the badge floats over every screen.
            devTooling?.Overlay()
            // </opt:devtools>
        }
    }

    AppTheme(
        mode = themeMode,
        hapticsEnabled =
            AppTheme.defaults.hapticsEnabled && settings.hapticsEnabled,
    ) {
        SystemBarIcons(dark = !AppTheme.colors.isLight)
        // <opt:applock>
        // Over the inspector too: what the app sent is as private as what it shows.
        AppLockGate(lock = appLock, modifier = Modifier.fillMaxSize(), content = screens)
        // </opt:applock>
        // <opt-else:applock>
        //~ screens()
        // </opt-else:applock>
    }
}

@Composable
private fun AppStack(
    navigator: AppNavigator,
    registry: NavRegistry,
    serialization: NavKeySerialization,
    pendingDeepLink: PendingDeepLink,
    // <opt:devtools>
    devTooling: DevTooling?,
    // </opt:devtools>
) {
    val backStack =
        rememberAppBackStack(startKey = AppDestinations.start, serialization = serialization)
    AppNavigationHost(backStack = backStack, registry = registry, navigator = navigator)
    OpenDeepLinks(stack = backStack, pending = pendingDeepLink)
    // <opt:devtools>
    if (devTooling != null) {
        ReportCurrentScreen(
            top = { backStack.current },
            serialization = serialization,
            tooling = devTooling,
        )
    }
    // </opt:devtools>
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

// <opt:appstatus>
/**
 * Reads the status in a scope of its own: a change recomposes the gate alone, not the root's whole
 * content.
 */
@Composable
private fun ApiStatusGate(signals: ApiSignals) {
    val status by signals.status.collectAsStateWithLifecycle()
    AppStatusGate(status = status, onRetry = signals::retry)
}
// </opt:appstatus>
