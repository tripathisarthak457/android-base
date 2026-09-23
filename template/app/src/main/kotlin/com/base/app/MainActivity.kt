package com.base.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.base.app.core.datastore.AppSettings
import com.base.app.core.datastore.AppSettingsStore
import com.base.app.core.datastore.AuthTokenStore
import com.base.app.core.navigation.AppNavKey
import com.base.app.core.navigation.AppNavigator
import com.base.app.core.navigation.NavKeySerialization
import com.base.app.core.navigation.NavRegistry
import com.base.app.core.navigation.ShellTab
import com.base.app.session.SessionCoordinator
import com.base.app.ui.AppDestinations
import com.base.app.ui.AppRoot
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
// <opt:deeplink>
import com.base.app.deeplink.DeepLinkResolver
// </opt:deeplink>
// <opt:onboarding>
import com.base.app.feature.onboarding.OnboardingKey
// </opt:onboarding>
// <opt:devtools>
import com.base.app.core.devtools.DevEnvironment
import com.base.app.core.devtools.DevToolsLog
// </opt:devtools>
// <opt:applock>
import android.view.WindowManager
import com.base.app.lock.AppLock
import com.base.app.lock.LockActivity
// </opt:applock>
// <opt:applock|deeplink>
import android.content.Intent
// </opt:applock|deeplink>
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

/**
 * The only Activity.
 *
 * ## The splash is held until the app knows what to draw
 *
 * `setKeepOnScreenCondition` keeps the system splash up until the first settings emission has
 * arrived, so the app never renders one frame in the wrong theme before correcting itself. The
 * condition is a plain flag rather than a `runBlocking` read: blocking the main thread during
 * startup is precisely what the splash screen API exists to avoid.
 *
 * ## Edge-to-edge, once
 *
 * Called before `setContent` so the first composed frame already knows the window's insets.
 * Doing it afterwards makes the content jump as the insets are applied.
 *
 * ## The entry point is derived, not navigated to
 *
 * Which screen the app opens on is a function of the settings it just read, so finishing
 * onboarding moves the app on by writing one flag — the same emission that repaints the theme.
 * The alternative, an imperative `navigate()` at startup, has to decide what to do when the flag
 * changes for any other reason, and gets it wrong on the second device the account signs in on.
 */
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

    // <opt:deeplink>
    @Inject
    lateinit var deepLinkResolver: DeepLinkResolver
    // </opt:deeplink>

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

    private val startup = MutableStateFlow<Startup?>(null)

    // <opt:language>
    // Below Android 13 the app's own language is applied here, before any resource is read.
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocales.wrap(newBase))
    }
    // </opt:language>

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        splash.setKeepOnScreenCondition { startup.value == null }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // <opt:applock>
        // Keeps the app's content out of the task switcher's thumbnail, and out of
        // screenshots. A lock that leaves the last screen legible in the recents list is a
        // lock in name only. It also blocks the user's own screenshots, which is the trade
        // every app with a lock makes — delete this line if it is the wrong one for yours.
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        // </opt:applock>

        lifecycleScope.launch {
            combine(
                settingsStore.settings,
                tokenStore.isAuthenticated,
                ::Startup,
            ).collect { startup.value = it }
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
                // <opt:devtools>
                devEnvironment = devEnvironment,
                devToolsLog = devToolsLog,
                // </opt:devtools>
            )
        }

        // <opt:playstore>
        // Both are tied to the Activity being resumed rather than to onCreate: the update check
        // is worth repeating when somebody comes back after a week, and a review sheet launched
        // against a stopped Activity is dropped by Play without a word.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launch { appUpdates.updates().collect(::onUpdate) }
                launch { reviewPrompt.onLaunch(this@MainActivity) }
            }
        }
        // </opt:playstore>

        // <opt:deeplink>
        // The launch intent is consumed once the host exists, so the deep link lands on top of
        // the start destination rather than replacing it — pressing Back from a link then goes
        // somewhere sensible instead of straight out of the app.
        handleDeepLink(intent)
        // </opt:deeplink>
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
     *
     * The check is asynchronous because the preference lives in a DataStore, and a blocking read
     * on the main thread during startup is the thing the splash screen API exists to avoid. That
     * leaves a window of one or two frames; FLAG_SECURE below is what covers it, by keeping this
     * Activity out of the task switcher's screenshot whether it is locked or not.
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

    // <opt:deeplink>
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // A second link while the app is already running arrives here, not in onCreate. Missing
        // this override is why "tapping a notification does nothing when the app is open" is
        // such a common bug.
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val key = deepLinkResolver.resolve(intent?.data) ?: return
        navigator.navigate(key)
    }
    // </opt:deeplink>
}

/** Everything the first frame depends on. The splash is held until all of it has arrived. */
private data class Startup(
    val settings: AppSettings,
    val signedIn: Boolean,
)

/**
 * Where this launch starts, and whether it has a tab bar.
 *
 * Read top to bottom, it is the order the gates come in: onboarding, then sign-in, then the app.
 * Both gates run in a single stack with no bottom bar — showing tabs over a walkthrough or a
 * login form invites the user to leave it half-finished, and the app then has to decide what a
 * partially onboarded, partly signed-in account means.
 *
 * Because it is a function of state the Activity already collects, finishing either gate is a
 * write to a store rather than a `navigate()` — so there is no path where the flag says one thing
 * and the back stack says another.
 */
private fun Startup.entryPoint(): Pair<AppNavKey, List<ShellTab>> {
    // <opt:onboarding>
    if (!settings.onboardingCompleted) return OnboardingKey to emptyList()
    // </opt:onboarding>
    // <opt:auth>
    if (!signedIn) return AppDestinations.signIn to emptyList()
    // </opt:auth>
    return AppDestinations.start to AppDestinations.tabs
}
