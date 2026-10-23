package io.example.fitclocal.ui

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
import io.example.fitclocal.core.common.navigation.AppNavKey
import io.example.fitclocal.core.datastore.settings.AppSettings
import io.example.fitclocal.core.designsystem.theme.AppTheme
import io.example.fitclocal.core.designsystem.theme.ThemeMode
import io.example.fitclocal.core.navigation.AppNavigationHost
import io.example.fitclocal.core.navigation.AppNavigator
import io.example.fitclocal.core.navigation.AppShell
import io.example.fitclocal.core.navigation.AppShortcuts
import io.example.fitclocal.core.navigation.HostCrossfade
import io.example.fitclocal.core.navigation.LocalAppShortcuts
import io.example.fitclocal.core.navigation.NavKeySerialization
import io.example.fitclocal.core.navigation.NavRegistry
import io.example.fitclocal.core.navigation.OpenDeepLinks
import io.example.fitclocal.core.navigation.PendingDeepLink
import io.example.fitclocal.core.navigation.ShellConfig
import io.example.fitclocal.core.navigation.rememberAppBackStack
import io.example.fitclocal.core.navigation.rememberShellState
import io.example.fitclocal.startup.EntryIdentity
import io.example.fitclocal.startup.ShellObserver
import io.example.fitclocal.startup.Startup
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
                )
            }
        }
    }

    AppTheme(mode = themeMode, hapticsEnabled = AppTheme.defaults.hapticsEnabled && settings.hapticsEnabled) {
        SystemBarIcons(dark = !AppTheme.colors.isLight)
        screens()
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
) {
    HostCrossfade(target = identity) { shown, receivesCommands ->
        when (shown) {
            EntryIdentity.Onboarding -> SingleStack(
                AppDestinations.onboarding, navigator, registry, serialization, shellConfig, receivesCommands,
            )
            EntryIdentity.SignedOut -> SingleStack(
                AppDestinations.signIn, navigator, registry, serialization, shellConfig, receivesCommands,
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
) {
    val backStack = rememberAppBackStack(startKey = startKey, serialization = serialization)
    AppNavigationHost(
        backStack = backStack,
        registry = registry,
        navigator = navigator,
        config = config,
        receivesCommands = receivesCommands,
    )
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
) {
    val tabs = AppDestinations.tabs
    if (tabs.isEmpty()) {
        SingleStack(
            AppDestinations.start, navigator, registry, serialization, shellConfig, receivesCommands,
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

