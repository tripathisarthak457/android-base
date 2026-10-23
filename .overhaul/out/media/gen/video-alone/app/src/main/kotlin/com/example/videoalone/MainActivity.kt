package com.example.videoalone

import android.annotation.SuppressLint
import android.app.UiModeManager
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.KeyboardShortcutGroup
import android.view.Menu
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.withFrameNanos
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.videoalone.core.datastore.settings.AppSettings
import com.example.videoalone.core.datastore.settings.AppSettingsStore
import com.example.videoalone.core.navigation.AppNavigator
import com.example.videoalone.core.navigation.AppShortcuts
import com.example.videoalone.core.navigation.NavKeySerialization
import com.example.videoalone.core.navigation.NavRegistry
import com.example.videoalone.core.navigation.PendingDeepLink
import com.example.videoalone.core.navigation.ShellConfig
import com.example.videoalone.startup.ShellConfigOverride
import com.example.videoalone.startup.ShellObserver
import com.example.videoalone.startup.StartupViewModel
import com.example.videoalone.ui.AppRoot
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** The only Activity. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var navigator: AppNavigator

    @Inject
    lateinit var registry: NavRegistry

    @Inject
    lateinit var serialization: NavKeySerialization

    @Inject
    lateinit var settingsStore: AppSettingsStore

    @Inject
    lateinit var pendingDeepLink: PendingDeepLink

    @Inject
    lateinit var shellConfigOverrides: Set<@JvmSuppressWildcards ShellConfigOverride>

    @Inject
    lateinit var shellObservers: Set<@JvmSuppressWildcards ShellObserver>

    private val startupViewModel: StartupViewModel by viewModels()

    private val shortcuts = AppShortcuts()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        splash.setKeepOnScreenCondition { startupViewModel.startup.value == null }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        startupViewModel.onLaunch(intent)
        followThemeOutsideTheApp()

        setContent {
            val startup by startupViewModel.startup.collectAsStateWithLifecycle()
            val current = startup ?: return@setContent
            // From its first frame on, the app paints every pixel of the window, so the window's own
            // background would be one more full-screen fill under every frame the GPU draws. It is
            // kept until then, as what shows while the app launches. A transparent ColorDrawable draws
            // nothing; changing its colour, unlike replacing the drawable, requests no layout, and the
            // mutate() keeps the change out of every other window built from the theme.
            LaunchedEffect(Unit) {
                withFrameNanos {}
                (window.decorView.background?.mutate() as? ColorDrawable)?.color = Color.TRANSPARENT
            }
            AppRoot(
                startup = current,
                navigator = navigator,
                registry = registry,
                serialization = serialization,
                pendingDeepLink = pendingDeepLink,
                shellConfig = shellConfigOverrides.fold(ShellConfig()) { config, override -> override.adjust(config) },
                shellObservers = shellObservers,
                shortcuts = shortcuts,
            )
        }

        // Only a fresh launch carries a new link. A recreation (rotation, theme, language) restores
        // the stack the link already built, and a launch from Recents replays the original intent:
        // held again, either would open the screen a second time.
        val fromHistory = intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0
        if (savedInstanceState == null && !fromHistory) pendingDeepLink.hold(intent.link())
    }

    /**
     * The system draws the splash and its own dialogs from the app's night mode, before any of the
     * app runs; told the in-app theme, a person who chose Dark gets a dark splash, not a light flash.
     * The activity takes the change in place (`uiMode` in the manifest's configChanges), so applying
     * it, or the person changing the theme, never recreates the screen.
     */
    private fun followThemeOutsideTheApp() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        lifecycleScope.launch {
            settingsStore.settings.map { it.themeMode }.distinctUntilChanged().collect { mode ->
                getSystemService(UiModeManager::class.java)?.setApplicationNightMode(
                    when (mode) {
                        AppSettings.THEME_DARK -> UiModeManager.MODE_NIGHT_YES
                        AppSettings.THEME_LIGHT -> UiModeManager.MODE_NIGHT_NO
                        else -> UiModeManager.MODE_NIGHT_AUTO
                    },
                )
            }
        }
    }

    /**
     * Keyboard shortcuts first, whatever has focus: Ctrl+1 must work while a text field has it.
     *
     * Lint reports this public Activity method as androidx-only because the restricted base class
     * androidx.core.app.ComponentActivity overrides it; an app overriding it and calling super is
     * what the framework method is for.
     */
    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean = shortcuts.handle(event) || super.dispatchKeyEvent(event)

    /** What the system's Shortcuts Helper (Meta+/) lists for the app. */
    override fun onProvideKeyboardShortcuts(data: MutableList<KeyboardShortcutGroup>, menu: Menu?, deviceId: Int) {
        super.onProvideKeyboardShortcuts(data, menu, deviceId)
        data += shortcuts.group(getString(R.string.app_name))
    }

    /** singleTop: a link or a notification tapped while the app is running arrives here. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingDeepLink.hold(intent.link())
    }

    private fun Intent.link(): Uri? = data
}
