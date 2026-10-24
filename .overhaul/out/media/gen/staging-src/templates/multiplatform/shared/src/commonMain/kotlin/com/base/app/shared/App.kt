package com.base.app.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.base.app.core.datastore.settings.AppSettings
import com.base.app.core.datastore.settings.AppSettingsStore
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.ThemeMode
import com.base.app.core.navigation.AppNavigator
import com.base.app.core.navigation.AppShell
import com.base.app.core.navigation.NavKeySerialization
import com.base.app.core.navigation.NavRegistry
import com.base.app.core.navigation.rememberShellState
import com.base.app.shared.navigation.AppDestinations
import org.koin.compose.koinInject
// <opt:onboarding>
import com.base.app.core.navigation.AppNavigationHost
import com.base.app.core.navigation.rememberAppBackStack
import com.base.app.feature.onboarding.api.OnboardingKey
// </opt:onboarding>

/**
 * The whole app, on both platforms: the theme the person chose, then onboarding until it is done
 * and the tabbed shell after. Which host shows is state read from the settings store, never a
 * navigation reset, so a finished onboarding cannot be reached again with Back.
 *
 * @param onDarkThemeChange called with whether the app is drawn dark, at start and on each change,
 *   so the platform can draw its system bar icons for the app's theme rather than the device's.
 */
@Composable
fun App(onDarkThemeChange: (dark: Boolean) -> Unit = {}) {
    val settingsStore = koinInject<AppSettingsStore>()
    val settings by settingsStore.settings.collectAsStateWithLifecycle(initialValue = settingsStore.current)
    val mode = settings?.themeMode.toThemeMode()

    val dark = mode.isDark()
    val currentOnDarkThemeChange by rememberUpdatedState(onDarkThemeChange)
    LaunchedEffect(dark) { currentOnDarkThemeChange(dark) }

    AppTheme(mode = mode) {
        when {
            // The store's first read is in flight: a few milliseconds of the launch background.
            settings == null -> Box(Modifier.fillMaxSize().background(AppTheme.colors.background))
            // <opt:onboarding>
            settings?.onboardingCompleted == false -> OnboardingHost()
            // </opt:onboarding>
            else -> ShellHost()
        }
    }
}

@Composable
private fun ShellHost() {
    val serialization = koinInject<NavKeySerialization>()
    val tabs = AppDestinations.tabs
    AppShell(
        tabs = tabs,
        state = rememberShellState(tabs, serialization),
        registry = koinInject<NavRegistry>(),
        navigator = koinInject<AppNavigator>(),
    )
}

// <opt:onboarding>
@Composable
private fun OnboardingHost() {
    AppNavigationHost(
        backStack = rememberAppBackStack(OnboardingKey, koinInject<NavKeySerialization>()),
        registry = koinInject<NavRegistry>(),
        navigator = koinInject<AppNavigator>(),
    )
}
// </opt:onboarding>

private fun String?.toThemeMode(): ThemeMode = when (this) {
    AppSettings.THEME_LIGHT -> ThemeMode.Light
    AppSettings.THEME_DARK -> ThemeMode.Dark
    else -> ThemeMode.System
}
