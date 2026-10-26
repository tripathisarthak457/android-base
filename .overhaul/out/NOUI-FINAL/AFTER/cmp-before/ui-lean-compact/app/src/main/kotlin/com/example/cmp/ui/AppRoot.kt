package com.example.cmp.ui

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
import com.example.cmp.core.common.navigation.AppNavKey
import com.example.cmp.core.datastore.settings.AppSettings
import com.example.cmp.core.designsystem.theme.AppTheme
import com.example.cmp.core.designsystem.theme.ThemeMode
import com.example.cmp.core.navigation.AppNavigator
import com.example.cmp.core.navigation.AppShortcuts
import com.example.cmp.core.navigation.LocalAppShortcuts
import com.example.cmp.core.navigation.NavKeySerialization
import com.example.cmp.core.navigation.NavRegistry
import com.example.cmp.core.navigation.PendingDeepLink
import com.example.cmp.core.navigation.rememberAppBackStack
import com.example.cmp.core.navigation.shell.AppNavigationHost
import com.example.cmp.core.navigation.shell.AppShell
import com.example.cmp.core.navigation.shell.HostCrossfade
import com.example.cmp.core.navigation.shell.OpenDeepLinks
import com.example.cmp.core.navigation.shell.ShellConfig
import com.example.cmp.core.navigation.shell.rememberShellState
import com.example.cmp.startup.EntryIdentity
import com.example.cmp.startup.ShellObserver
import com.example.cmp.startup.Startup
import kotlinx.coroutines.launch

/** Everything above the navigation hosts: the theme, the system bars, and which app is showing. */
@Composable
internal fun AppRoot(
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
        // Test tags become resource ids, which is how UI Automator and Macrobenchmark find tabs
        // and lists.
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

