package com.example.wp04.base

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.wp04.base.core.datastore.AppSettings
import com.example.wp04.base.core.datastore.AppSettingsStore
import com.example.wp04.base.core.datastore.AuthTokenStore
import com.example.wp04.base.core.navigation.AppNavKey
import com.example.wp04.base.core.navigation.AppNavigator
import com.example.wp04.base.core.navigation.NavKeySerialization
import com.example.wp04.base.core.navigation.NavRegistry
import com.example.wp04.base.core.navigation.ShellTab
import com.example.wp04.base.session.SessionCoordinator
import com.example.wp04.base.ui.AppDestinations
import com.example.wp04.base.ui.AppRoot
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.wp04.base.deeplink.DeepLinkResolver
import com.example.wp04.base.core.devtools.DevEnvironment
import com.example.wp04.base.core.devtools.DevToolsLog
import android.content.Intent
import android.content.Context
import com.example.wp04.base.core.ui.locale.AppLocales

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
    lateinit var devEnvironment: DevEnvironment

    @Inject
    lateinit var devToolsLog: DevToolsLog

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
    if (!signedIn) return AppDestinations.signIn to emptyList()
    return AppDestinations.start to AppDestinations.tabs
}
