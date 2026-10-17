package com.base.app

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
import com.base.app.core.datastore.AppSettings
import com.base.app.core.datastore.AppSettingsStore
import com.base.app.core.navigation.AppNavigator
import com.base.app.core.navigation.AppShortcuts
import com.base.app.core.navigation.NavKeySerialization
import com.base.app.core.navigation.NavRegistry
import com.base.app.core.navigation.PendingDeepLink
import com.base.app.core.navigation.ShellConfig
import com.base.app.startup.ShellConfigOverride
import com.base.app.startup.ShellObserver
import com.base.app.startup.StartupViewModel
import com.base.app.ui.AppRoot
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
// <opt:devtools>
import com.base.app.core.devtools.DevEnvironment
import com.base.app.core.devtools.DevToolsLog
// </opt:devtools>
// <opt:applock>
import android.view.WindowManager
import com.base.app.lock.AppLock
import com.base.app.lock.LockActivity
// </opt:applock>
// <opt:language>
import android.content.Context
import com.base.app.core.ui.locale.AppLocales
// </opt:language>
// <opt:playstore>
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.base.app.playstore.AppUpdates
import com.base.app.playstore.ReviewPrompt
import com.google.android.play.core.ktx.AppUpdateResult
// </opt:playstore>

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

    // <opt:applock>
    @Inject
    lateinit var appLock: AppLock
    // </opt:applock>

    // <opt:devtools>
    @Inject
    lateinit var devEnvironment: DevEnvironment

    @Inject
    lateinit var devToolsLog: DevToolsLog
    // </opt:devtools>

    // <opt:playstore>
    @Inject
    lateinit var appUpdates: AppUpdates

    @Inject
    lateinit var reviewPrompt: ReviewPrompt
    // </opt:playstore>

    private val startupViewModel: StartupViewModel by viewModels()

    private val shortcuts = AppShortcuts()

    // <opt:language>
    // Below Android 13 the app's own language is applied here, before any resource is read.
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocales.wrap(newBase))
    }
    // </opt:language>

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        splash.setKeepOnScreenCondition { startupViewModel.startup.value == null }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        startupViewModel.onLaunch(intent)
        followThemeOutsideTheApp()
        // <opt:applock>

        // With the lock on, the app's content stays out of the task switcher's thumbnail and out of
        // screenshots: a lock that leaves the last screen legible in recents is a lock in name only.
        // With it off, people can screenshot and screen-share the app like any other.
        lifecycleScope.launch {
            settingsStore.settings.map { it.appLockEnabled }.distinctUntilChanged().collect { locked ->
                if (locked) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
        }
        // </opt:applock>

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
                // <opt:devtools>
                devEnvironment = devEnvironment,
                devToolsLog = devToolsLog,
                // </opt:devtools>
            )
        }

        // <opt:playstore>
        // On resume rather than onCreate: Play drops a review sheet launched against a stopped
        // Activity.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launch { appUpdates.updates().collect(::onUpdate) }
                launch { reviewPrompt.onLaunch(this@MainActivity) }
            }
        }
        // </opt:playstore>

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

    // <opt:playstore>
    private suspend fun onUpdate(result: AppUpdateResult) {
        when (result) {
            is AppUpdateResult.Available -> appUpdates.start(result, this)
            // Downloaded and waiting for a restart. Asking rather than restarting under the
            // user: an app that relaunches itself mid-sentence loses whatever they were doing.
            is AppUpdateResult.Downloaded -> appUpdates.install(result)
            is AppUpdateResult.InProgress, AppUpdateResult.NotAvailable -> Unit
        }
    }
    // </opt:playstore>

    // <opt:applock>
    /**
     * The lock is decided in `onStart`, before the window is drawn, so the app's own content is
     * never on screen — even for a frame — while it is meant to be covered.
     */
    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            if (appLock.shouldLock()) {
                startActivity(Intent(this@MainActivity, LockActivity::class.java))
            }
        }
    }

    override fun onStop() {
        super.onStop()
        appLock.onHidden()
    }
    // </opt:applock>

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

    // <opt:deeplink>
    /** A link forwarded by LinkActivity, or the data of an intent sent here directly (a notification). */
    private fun Intent.link(): Uri? = getStringExtra(EXTRA_LINK)?.let(Uri::parse) ?: data

    companion object {
        const val EXTRA_LINK = "com.base.app.extra.LINK"
    }
    // </opt:deeplink>
    // <opt:!deeplink>    private fun Intent.link(): Uri? = data
}
