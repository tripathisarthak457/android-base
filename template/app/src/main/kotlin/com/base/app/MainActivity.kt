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
        // Keeps the app's content out of the task switcher's thumbnail, and out of screenshots. A
        // lock that leaves the last screen legible in the recents list is a lock in name only.
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
        // On resume rather than onCreate: Play drops a review sheet launched against a stopped
        // Activity.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launch { appUpdates.updates().collect(::onUpdate) }
                launch { reviewPrompt.onLaunch(this@MainActivity) }
            }
        }
        // </opt:playstore>

        // <opt:deeplink>
        // Handled after the host exists, so the link lands on top of the start destination and Back
        // works.
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
        // A link that arrives while the app is running comes here, not to onCreate.
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

/** Where this launch starts, and whether it has a tab bar. */
private fun Startup.entryPoint(): Pair<AppNavKey, List<ShellTab>> {
    // <opt:onboarding>
    if (!settings.onboardingCompleted) return OnboardingKey to emptyList()
    // </opt:onboarding>
    // <opt:auth>
    if (!signedIn) return AppDestinations.signIn to emptyList()
    // </opt:auth>
    return AppDestinations.start to AppDestinations.tabs
}
