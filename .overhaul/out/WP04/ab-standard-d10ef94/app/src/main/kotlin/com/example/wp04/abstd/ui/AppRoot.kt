package com.example.wp04.abstd.ui

import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.example.wp04.abstd.core.common.navigation.AppNavKey
import com.example.wp04.abstd.core.datastore.AppSettings
import com.example.wp04.abstd.core.devtools.DevEnvironment
import com.example.wp04.abstd.core.devtools.DevToolsOverlay
import com.example.wp04.abstd.core.devtools.DevToolsLog
import com.example.wp04.abstd.core.designsystem.theme.AppDesignStyle
import com.example.wp04.abstd.core.designsystem.theme.AppMotionStyle
import com.example.wp04.abstd.core.designsystem.theme.AppTheme
import com.example.wp04.abstd.core.designsystem.theme.ThemeMode
import com.example.wp04.abstd.core.navigation.AppNavigationHost
import com.example.wp04.abstd.core.navigation.AppNavigator
import com.example.wp04.abstd.core.navigation.AppShortcuts
import com.example.wp04.abstd.core.navigation.AppShell
import com.example.wp04.abstd.core.navigation.LocalAppShortcuts
import com.example.wp04.abstd.core.navigation.NavKeySerialization
import com.example.wp04.abstd.core.navigation.NavRegistry
import com.example.wp04.abstd.core.navigation.OpenDeepLinks
import com.example.wp04.abstd.core.navigation.PendingDeepLink
import com.example.wp04.abstd.core.navigation.ShellConfig
import com.example.wp04.abstd.core.navigation.rememberAppBackStack
import com.example.wp04.abstd.core.navigation.rememberShellState
import com.example.wp04.abstd.startup.EntryIdentity
import com.example.wp04.abstd.startup.ShellObserver
import com.example.wp04.abstd.startup.Startup
import kotlinx.coroutines.launch

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
    devEnvironment: DevEnvironment,
    devToolsLog: DevToolsLog,
) {
    val settings = startup.settings
    val themeMode = settings.themeMode()

    AppTheme(
        mode = themeMode,
        motionStyle = AppMotionStyle.Standard,
        designStyle = AppDesignStyle.Utility,
        hapticsEnabled = settings.hapticsEnabled,
    ) {
        SystemBarIcons(dark = !AppTheme.colors.isLight)
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
                )
            }

            // Last in the Box so the badge floats over every screen; a no-op in production builds.
            DevToolsOverlay(environment = devEnvironment, log = devToolsLog)
        }
    }
}

/**
 * Onboarding, the signed-out screens and the signed-in shell are separate hosts. Moving between them
 * is the one whole-screen crossfade in the app, at most once per session; a Back pressed during it
 * is swallowed, since it would act on a host that is leaving.
 */
@Composable
private fun EntryHost(
    identity: EntryIdentity,
    navigator: AppNavigator,
    registry: NavRegistry,
    serialization: NavKeySerialization,
    pendingDeepLink: PendingDeepLink,
    shellConfig: ShellConfig,
    shellObservers: Set<ShellObserver>,
) {
    val swap = updateTransition(targetState = identity, label = "identity")
    val background = AppTheme.colors.background
    swap.AnimatedContent(
        // The window has no background of its own once the app has drawn (MainActivity), and two
        // hosts half faded in and out would show it through each other.
        modifier = Modifier.drawBehind { if (swap.isRunning) drawRect(background) },
        transitionSpec = { fadeIn(tween(IDENTITY_FADE_MS)) togetherWith fadeOut(tween(IDENTITY_FADE_MS)) },
    ) { shown ->
        when (shown) {
            EntryIdentity.Onboarding -> SingleStack(AppDestinations.onboarding, navigator, registry, serialization, shellConfig)
            EntryIdentity.SignedOut -> SingleStack(AppDestinations.signIn, navigator, registry, serialization, shellConfig)
            is EntryIdentity.SignedIn -> SignedIn(
                generation = shown.generation,
                navigator = navigator,
                registry = registry,
                serialization = serialization,
                pendingDeepLink = pendingDeepLink,
                shellConfig = shellConfig,
                shellObservers = shellObservers,
            )
        }
    }
    val idle = rememberNavigationEventState(NavigationEventInfo.None)
    NavigationBackHandler(state = idle, isBackEnabled = swap.isRunning, onBackCompleted = {})
}

@Composable
private fun SingleStack(
    startKey: AppNavKey,
    navigator: AppNavigator,
    registry: NavRegistry,
    serialization: NavKeySerialization,
    config: ShellConfig,
) {
    val backStack = rememberAppBackStack(startKey = startKey, serialization = serialization)
    AppNavigationHost(backStack = backStack, registry = registry, navigator = navigator, config = config)
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
) {
    val tabs = AppDestinations.tabs
    if (tabs.isEmpty()) {
        SingleStack(AppDestinations.start, navigator, registry, serialization, shellConfig)
        return
    }
    val shellState = rememberShellState(tabs = tabs, registry = registry, serialization = serialization, generation = generation)
    OpenDeepLinks(state = shellState, pending = pendingDeepLink)
    LaunchedEffect(shellState, shellObservers) {
        shellObservers.forEach { observer -> launch { observer.observe(shellState) } }
    }
    AppShell(tabs = tabs, state = shellState, registry = registry, navigator = navigator, config = shellConfig)
}

/**
 * Status and navigation bar icons follow the app's theme rather than the system's: an app set to
 * Dark on a light phone would otherwise draw dark icons on a dark screen.
 */
@Composable
private fun SystemBarIcons(dark: Boolean) {
    val activity = LocalActivity.current as? ComponentActivity ?: return
    LaunchedEffect(activity, dark) {
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

private const val IDENTITY_FADE_MS = 200
