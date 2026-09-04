package com.base.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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
import android.content.Intent
import com.base.app.deeplink.DeepLinkResolver
// </opt:deeplink>
// <opt:onboarding>
import com.base.app.feature.onboarding.OnboardingKey
// </opt:onboarding>

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

    private val startup = MutableStateFlow<Startup?>(null)

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

        setContent {
            val current by startup.collectAsState()
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
            )
        }

        // <opt:deeplink>
        // The launch intent is consumed once the host exists, so the deep link lands on top of
        // the start destination rather than replacing it — pressing Back from a link then goes
        // somewhere sensible instead of straight out of the app.
        handleDeepLink(intent)
        // </opt:deeplink>
    }

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
