package com.example.hud.fixture

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.hud.fixture.core.common.navigation.AppNavKey
import com.example.hud.fixture.core.datastore.settings.AppSettings
import com.example.hud.fixture.core.datastore.settings.AppSettingsStore
import com.example.hud.fixture.core.datastore.session.AuthTokenStore
import com.example.hud.fixture.core.navigation.AppNavigator
import com.example.hud.fixture.core.navigation.NavKeySerialization
import com.example.hud.fixture.core.navigation.NavRegistry
import com.example.hud.fixture.core.navigation.ShellTab
import com.example.hud.fixture.session.SessionCoordinator
import com.example.hud.fixture.ui.AppDestinations
import com.example.hud.fixture.ui.AppRoot
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.hud.fixture.deeplink.DeepLinkResolver
import com.example.hud.fixture.feature.onboarding.api.OnboardingKey
import com.example.hud.fixture.core.ui.tooling.DevTooling
import java.util.Optional
import kotlin.jvm.optionals.getOrNull
import android.view.WindowManager
import androidx.fragment.app.FragmentActivity
import com.example.hud.fixture.core.applock.AppLock
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import android.content.Intent
import android.content.Context
import com.example.hud.fixture.core.ui.locale.AppLocales
import com.example.hud.fixture.core.network.appstatus.ApiSignals
import com.example.hud.fixture.core.playstore.review.ReviewPrompt
import com.example.hud.fixture.core.playstore.update.AppUpdates
import com.example.hud.fixture.core.analytics.jank.FrameJankRecorder

/** The only Activity. */
@AndroidEntryPoint
// A FragmentActivity only because BiometricPrompt runs in one.
class MainActivity : FragmentActivity() {
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

    // Present in dev-flavour and debug builds, the only ones that depend on :core:devtools.
    @Inject
    lateinit var devTooling: Optional<DevTooling>

    @Inject
    lateinit var apiSignals: ApiSignals

    @Inject
    lateinit var appUpdates: AppUpdates

    @Inject
    lateinit var reviewPrompt: ReviewPrompt

    @Inject
    lateinit var frameJank: FrameJankRecorder

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
        frameJank.track(window, lifecycle)

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
                devTooling = devTooling.getOrNull(),
                appLock = appLock,
                apiSignals = apiSignals,
                appUpdates = appUpdates,
                reviewPrompt = reviewPrompt,
            )
        }

        // Handled after the host exists, so the link lands on top of the start destination and Back
        // works.
        handleDeepLink(intent)
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
