package com.base.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.base.app.core.common.navigation.AppNavKey
import com.base.app.core.datastore.settings.AppSettings
import com.base.app.core.datastore.settings.AppSettingsStore
import com.base.app.core.datastore.session.AuthTokenStore
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
import com.base.app.feature.onboarding.api.OnboardingKey
// </opt:onboarding>
// <opt:devtools>
import com.base.app.core.devtools.DevTools
// </opt:devtools>
// <opt:applock>
import android.view.WindowManager
import androidx.fragment.app.FragmentActivity
import com.base.app.core.applock.AppLock
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
// </opt:applock>
// <opt-else:applock>
//~ import androidx.activity.ComponentActivity
// </opt-else:applock>
// <opt:deeplink>
import android.content.Intent
// </opt:deeplink>
// <opt:language>
import android.content.Context
import com.base.app.core.ui.locale.AppLocales
// </opt:language>
// <opt:appstatus>
import com.base.app.core.network.appstatus.ApiSignals
// </opt:appstatus>
// <opt:playstore>
import com.base.app.core.playstore.review.ReviewPrompt
import com.base.app.core.playstore.update.AppUpdates
// </opt:playstore>
// <opt:analytics>
import com.base.app.core.analytics.jank.FrameJankRecorder
// </opt:analytics>

/** The only Activity. */
@AndroidEntryPoint
// <opt:applock>
// A FragmentActivity only because BiometricPrompt runs in one.
class MainActivity : FragmentActivity() {
// </opt:applock>
// <opt:!applock>class MainActivity : ComponentActivity() {

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
    lateinit var devTools: DevTools
    // </opt:devtools>

    // <opt:appstatus>
    @Inject
    lateinit var apiSignals: ApiSignals
    // </opt:appstatus>

    // <opt:playstore>
    @Inject
    lateinit var appUpdates: AppUpdates

    @Inject
    lateinit var reviewPrompt: ReviewPrompt
    // </opt:playstore>

    // <opt:analytics>
    @Inject
    lateinit var frameJank: FrameJankRecorder
    // </opt:analytics>

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
        // <opt:analytics>
        frameJank.track(window, lifecycle)
        // </opt:analytics>

        lifecycleScope.launch {
            combine(
                settingsStore.settings,
                tokenStore.isAuthenticated,
                ::Startup,
            ).collect { startup.value = it }
        }
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
                devTools = devTools,
                // </opt:devtools>
                // <opt:applock>
                appLock = appLock,
                // </opt:applock>
                // <opt:appstatus>
                apiSignals = apiSignals,
                // </opt:appstatus>
                // <opt:playstore>
                appUpdates = appUpdates,
                reviewPrompt = reviewPrompt,
                // </opt:playstore>
            )
        }

        // <opt:deeplink>
        // Handled after the host exists, so the link lands on top of the start destination and Back
        // works.
        handleDeepLink(intent)
        // </opt:deeplink>
    }

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
