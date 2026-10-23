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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.base.app.core.common.navigation.AppNavKey
import com.base.app.core.datastore.settings.AppSettings
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.ThemeMode
import com.base.app.core.navigation.AppNavigationHost
import com.base.app.core.navigation.AppNavigator
import com.base.app.core.navigation.AppShortcuts
import com.base.app.core.navigation.AppShell
import com.base.app.core.navigation.HostCrossfade
import com.base.app.core.navigation.LocalAppShortcuts
import com.base.app.core.navigation.NavKeySerialization
import com.base.app.core.navigation.NavRegistry
import com.base.app.core.navigation.OpenDeepLinks
import com.base.app.core.navigation.PendingDeepLink
import com.base.app.core.navigation.ShellConfig
import com.base.app.core.navigation.rememberAppBackStack
import com.base.app.core.navigation.rememberShellState
// <opt:devtools>
import com.base.app.core.ui.tooling.DevTooling
// </opt:devtools>
import com.base.app.startup.EntryIdentity
import com.base.app.startup.ShellObserver
import com.base.app.startup.Startup
import kotlinx.coroutines.launch
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

/** Everything above the navigation hosts: the theme, the system bars, and which app is showing. */
@Composable
fun AppRoot(
    startup: Startup,
    navigator: AppNavigator,
    registry: NavRegistry,
    serialization: NavKeySerialization,
    pendingDeepLink: PendingDeepLink,
    shellConfig: ShellConfig,
    shellObservers: Set<ShellObserver>,
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
        // Test tags become resource ids, which is how benchmarks and tools/perf find tabs and lists.
        Box(modifier = Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }) {
            CompositionLocalProvider(LocalAppShortcuts provides shortcuts) {
                EntryHost(
                    identity = startup.identity,
                    navigator = navigator,
                    registry = registry,
                    serialization = serialization,
                    pendingDeepLink = pendingDeepLink,
                    shellConfig = shellConfig,
                    shellObservers = shellObservers,
                    // <opt:devtools>
                    devTooling = devTooling,
                    // </opt:devtools>
                )
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

    AppTheme(mode = themeMode, hapticsEnabled = AppTheme.defaults.hapticsEnabled && settings.hapticsEnabled) {
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

/** Onboarding, the signed-out screens and the signed-in shell are separate hosts. */
@Composable
private fun EntryHost(
    identity: EntryIdentity,
    navigator: AppNavigator,
    registry: NavRegistry,
    serialization: NavKeySerialization,
    pendingDeepLink: PendingDeepLink,
    shellConfig: ShellConfig,
    shellObservers: Set<ShellObserver>,
    // <opt:devtools>
    devTooling: DevTooling?,
    // </opt:devtools>
) {
    HostCrossfade(target = identity) { shown, receivesCommands ->
        when (shown) {
            EntryIdentity.Onboarding -> SingleStack(
                AppDestinations.onboarding, navigator, registry, serialization, shellConfig, receivesCommands,
                // <opt:devtools>
                devTooling = devTooling,
                // </opt:devtools>
            )
            EntryIdentity.SignedOut -> SingleStack(
                AppDestinations.signIn, navigator, registry, serialization, shellConfig, receivesCommands,
                // <opt:devtools>
                devTooling = devTooling,
                // </opt:devtools>
            )
            is EntryIdentity.SignedIn -> SignedIn(
                generation = shown.generation,
                navigator = navigator,
                registry = registry,
                serialization = serialization,
                pendingDeepLink = pendingDeepLink,
                shellConfig = shellConfig,
                shellObservers = shellObservers,
                receivesCommands = receivesCommands,
                // <opt:devtools>
                devTooling = devTooling,
                // </opt:devtools>
            )
        }
    }
}

@Composable
private fun SingleStack(
    startKey: AppNavKey,
    navigator: AppNavigator,
    registry: NavRegistry,
    serialization: NavKeySerialization,
    config: ShellConfig,
    receivesCommands: Boolean,
    // <opt:devtools>
    devTooling: DevTooling?,
    // </opt:devtools>
) {
    val backStack = rememberAppBackStack(startKey = startKey, serialization = serialization)
    AppNavigationHost(
        backStack = backStack,
        registry = registry,
        navigator = navigator,
        config = config,
        receivesCommands = receivesCommands,
    )
    // <opt:devtools>
    // Only the host the app is on reports: during the identity crossfade the departing one is still composed.
    if (devTooling != null && receivesCommands) {
        ReportCurrentScreen(top = { backStack.current }, serialization = serialization, tooling = devTooling)
    }
    // </opt:devtools>
}

@Composable
private fun SignedIn(
    generation: Int,
    navigator: AppNavigator,
    registry: NavRegistry,
    serialization: NavKeySerialization,
    pendingDeepLink: PendingDeepLink,
    shellConfig: ShellConfig,
    shellObservers: Set<ShellObserver>,
    receivesCommands: Boolean,
    // <opt:devtools>
    devTooling: DevTooling?,
    // </opt:devtools>
) {
    val tabs = AppDestinations.tabs
    if (tabs.isEmpty()) {
        SingleStack(
            AppDestinations.start, navigator, registry, serialization, shellConfig, receivesCommands,
            // <opt:devtools>
            devTooling = devTooling,
            // </opt:devtools>
        )
        return
    }
    val shellState = rememberShellState(tabs = tabs, registry = registry, serialization = serialization, generation = generation)
    OpenDeepLinks(state = shellState, pending = pendingDeepLink)
    LaunchedEffect(shellState, shellObservers) {
        shellObservers.forEach { observer -> launch { observer.observe(shellState) } }
    }
    AppShell(
        tabs = tabs,
        state = shellState,
        registry = registry,
        navigator = navigator,
        config = shellConfig,
        receivesCommands = receivesCommands,
    )
    // <opt:devtools>
    // The destination on top: a presented cover or sheet, else the front tab's top.
    if (devTooling != null && receivesCommands) {
        ReportCurrentScreen(
            top = { shellState.presentedKeys.lastOrNull() ?: shellState.current.current },
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
/** Reads the status in a scope of its own: a change recomposes the gate alone, not the root's whole content. */
@Composable
private fun ApiStatusGate(signals: ApiSignals) {
    val status by signals.status.collectAsStateWithLifecycle()
    AppStatusGate(status = status, onRetry = signals::retry)
}
// </opt:appstatus>
