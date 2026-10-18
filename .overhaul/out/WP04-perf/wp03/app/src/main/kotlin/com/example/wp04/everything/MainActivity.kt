package com.example.wp04.everything

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.wp04.everything.core.common.navigation.AppNavKey
import com.example.wp04.everything.core.datastore.AppSettings
import com.example.wp04.everything.core.datastore.AppSettingsStore
import com.example.wp04.everything.core.datastore.AuthTokenStore
import com.example.wp04.everything.core.navigation.AppNavigator
import com.example.wp04.everything.core.navigation.NavKeySerialization
import com.example.wp04.everything.core.navigation.NavRegistry
import com.example.wp04.everything.core.navigation.ShellTab
import com.example.wp04.everything.session.SessionCoordinator
import com.example.wp04.everything.ui.AppDestinations
import com.example.wp04.everything.ui.AppRoot
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.wp04.everything.deeplink.DeepLinkResolver
import com.example.wp04.everything.feature.onboarding.api.OnboardingKey
import com.example.wp04.everything.core.devtools.DevEnvironment
import com.example.wp04.everything.core.devtools.DevToolsLog
import android.view.WindowManager
import com.example.wp04.everything.lock.AppLock
import com.example.wp04.everything.lock.LockActivity
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import android.content.Intent
import android.content.Context
import com.example.wp04.everything.core.ui.locale.AppLocales
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.wp04.everything.playstore.AppUpdates
import com.example.wp04.everything.playstore.ReviewPrompt
import com.google.android.play.core.ktx.AppUpdateResult

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
    lateinit var sessionCoordinator: SessionCoordinator

    @Inject
    lateinit var settingsStore: AppSettingsStore

    @Inject
    lateinit var tokenStore: AuthTokenStore

    @Inject
    lateinit var deepLinkResolver: DeepLinkResolver

    @Inject
    lateinit var appLock: AppLock

    @Inject
    lateinit var devEnvironment: DevEnvironment

    @Inject
    lateinit var devToolsLog: DevToolsLog

    @Inject
    lateinit var appUpdates: AppUpdates

    @Inject
    lateinit var reviewPrompt: ReviewPrompt

    private val startup = MutableStateFlow<Startup?>(null)

    // Below Android 13 the app's own language is applied here, before any resource is read.
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocales.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        splash.setKeepOnScreenCondition { startup.value == null }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            combine(
                settingsStore.settings,
                tokenStore.isAuthenticated,
                ::Startup,
            ).collect { startup.value = it }
        }

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

        setContent {
            val current by startup.collectAsStateWithLifecycle()
            val resolved = current ?: return@setContent
            val (startKey, tabs) = resolved.entryPoint()

            AppRoot(
                startKey = startKey,
                tabs = tabs,
                navigator = navigator,
                registry = registry,
                serialization = serialization,
                sessionCoordinator = sessionCoordinator,
                settings = resolved.settings,
                signInKey = AppDestinations.signIn,
                onExitRequested = { finish() },
                devEnvironment = devEnvironment,
                devToolsLog = devToolsLog,
            )
        }

        // On resume rather than onCreate: Play drops a review sheet launched against a stopped
        // Activity.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launch { appUpdates.updates().collect(::onUpdate) }
                launch { reviewPrompt.onLaunch(this@MainActivity) }
            }
        }

        // Handled after the host exists, so the link lands on top of the start destination and Back
        // works.
        handleDeepLink(intent)
    }

    private suspend fun onUpdate(result: AppUpdateResult) {
        when (result) {
            is AppUpdateResult.Available -> appUpdates.start(result, this)
            // Downloaded and waiting for a restart. Asking rather than restarting under the
            // user: an app that relaunches itself mid-sentence loses whatever they were doing.
            is AppUpdateResult.Downloaded -> appUpdates.install(result)
            is AppUpdateResult.InProgress, AppUpdateResult.NotAvailable -> Unit
        }
    }

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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // A link that arrives while the app is running comes here, not to onCreate.
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val key = deepLinkResolver.resolve(intent?.data) ?: return
        navigator.navigate(key)
    }
}

/** Everything the first frame depends on. The splash is held until all of it has arrived. */
private data class Startup(
    val settings: AppSettings,
    val signedIn: Boolean,
)

/** Where this launch starts, and whether it has a tab bar. */
private fun Startup.entryPoint(): Pair<AppNavKey, List<ShellTab>> {
    if (!settings.onboardingCompleted) return OnboardingKey to emptyList()
    if (!signedIn) return AppDestinations.signIn to emptyList()
    return AppDestinations.start to AppDestinations.tabs
}
