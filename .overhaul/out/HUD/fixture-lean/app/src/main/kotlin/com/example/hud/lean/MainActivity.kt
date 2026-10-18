package com.example.hud.lean

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.hud.lean.core.common.navigation.AppNavKey
import com.example.hud.lean.core.datastore.settings.AppSettings
import com.example.hud.lean.core.datastore.settings.AppSettingsStore
import com.example.hud.lean.core.datastore.session.AuthTokenStore
import com.example.hud.lean.core.navigation.AppNavigator
import com.example.hud.lean.core.navigation.NavKeySerialization
import com.example.hud.lean.core.navigation.NavRegistry
import com.example.hud.lean.core.navigation.ShellTab
import com.example.hud.lean.session.SessionCoordinator
import com.example.hud.lean.ui.AppDestinations
import com.example.hud.lean.ui.AppRoot
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.activity.ComponentActivity

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
            )
        }
    }
}

/** Everything the first frame depends on. The splash is held until all of it has arrived. */
private data class Startup(
    val settings: AppSettings,
    val signedIn: Boolean,
)

/** Where this launch starts, and whether it has a tab bar. */
private fun Startup.entryPoint(): Pair<AppNavKey, List<ShellTab>> {
    return AppDestinations.start to AppDestinations.tabs
}
