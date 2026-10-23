package io.example.fitcnetdb.perf

import android.content.Intent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import io.example.fitcnetdb.core.datastore.session.AuthTokenStore
import io.example.fitcnetdb.core.datastore.session.AuthTokens
import io.example.fitcnetdb.core.datastore.settings.AppSettings
import io.example.fitcnetdb.core.datastore.settings.AppSettingsStore
import io.example.fitcnetdb.core.designsystem.component.navigation.BarMinimize
import io.example.fitcnetdb.core.designsystem.component.navigation.TabLabels
import io.example.fitcnetdb.core.navigation.ShellConfig
import io.example.fitcnetdb.startup.LaunchHook
import io.example.fitcnetdb.startup.ShellConfigOverride
import io.example.fitcnetdb.startup.ShellObserver
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * Launch extras for the benchmark journeys and tools/perf, read by dev builds only:
 *
 * - `perf.skipOnboarding=true` marks onboarding done;
 * - `perf.fakeSession=true` does that too and saves a synthetic session, so sign-in is skipped;
 * - `perf.screenResponse=0.30` sets the screen spring's response, in seconds, to compare candidates;
 * - `perf.barMinimize=OnScrollDown` makes the tab bar minimize while a list scrolls down;
 * - `perf.barLabels=Selected` (or `Never`) sets which tabs show their label;
 * - `perf.theme=dark` (or `light`, `system`) saves the app's theme, as the settings screen does, so
 *   a recording can be made in either without touching the phone's own setting.
 *
 * The token is made up. A dev server that checks tokens answers 401 and the app signs out, so the
 * journeys rely on screens that load without a real session, as the sample feature's public API does.
 */
@Singleton
class DevLaunchOptions @Inject constructor(
    private val settings: AppSettingsStore,
    private val tokens: AuthTokenStore,
) : LaunchHook, ShellConfigOverride {

    private var screenResponse: Float? = null
    private var barMinimize: BarMinimize? = null
    private var barLabels: TabLabels? = null

    override suspend fun beforeStart(intent: Intent) {
        val fakeSession = intent.isOn(FAKE_SESSION)
        if (fakeSession || intent.isOn(SKIP_ONBOARDING)) settings.setOnboardingCompleted(true)
        // Whether a session is stored, not what it holds: reading the token decrypts it through the
        // Keystore, binder calls on the main thread before the first frame that every measured cold
        // start would pay and no user's start does.
        if (fakeSession && !tokens.isAuthenticated.first()) tokens.save(AuthTokens(SYNTHETIC_TOKEN, SYNTHETIC_TOKEN, owner = SYNTHETIC_OWNER))
        intent.getStringExtra(THEME)?.takeIf { it in THEMES }?.let { settings.setThemeMode(it) }
        screenResponse = intent.getStringExtra(SCREEN_RESPONSE)?.toFloatOrNull()?.takeIf { it > 0f }
        barMinimize = intent.getStringExtra(BAR_MINIMIZE)?.let { name -> BarMinimize.entries.firstOrNull { it.name == name } }
        barLabels = intent.getStringExtra(BAR_LABELS)?.let { name -> TabLabels.entries.firstOrNull { it.name == name } }
    }

    override fun adjust(config: ShellConfig): ShellConfig = config.copy(
        screenResponse = screenResponse ?: config.screenResponse,
        barMinimize = barMinimize ?: config.barMinimize,
        barLabels = barLabels ?: config.barLabels,
    )

    private fun Intent.isOn(name: String): Boolean = getStringExtra(name).toBoolean()

    private companion object {
        const val SKIP_ONBOARDING = "perf.skipOnboarding"
        const val FAKE_SESSION = "perf.fakeSession"
        const val SCREEN_RESPONSE = "perf.screenResponse"
        const val BAR_MINIMIZE = "perf.barMinimize"
        const val BAR_LABELS = "perf.barLabels"
        const val THEME = "perf.theme"
        val THEMES = setOf(AppSettings.THEME_SYSTEM, AppSettings.THEME_LIGHT, AppSettings.THEME_DARK)
        const val SYNTHETIC_TOKEN = "perf-session"
        const val SYNTHETIC_OWNER = "perf-session-owner"
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface DevLaunchOptionsModule {

    @Binds
    @IntoSet
    fun launchHook(options: DevLaunchOptions): LaunchHook

    @Binds
    @IntoSet
    fun shellConfigOverride(options: DevLaunchOptions): ShellConfigOverride

    @Binds
    @IntoSet
    fun shellLog(log: ShellLog): ShellObserver
}
