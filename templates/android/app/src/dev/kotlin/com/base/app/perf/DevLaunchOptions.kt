package com.base.app.perf

import android.content.Intent
import com.base.app.core.datastore.AppSettingsStore
import com.base.app.core.datastore.AuthTokenStore
import com.base.app.core.designsystem.component.navigation.BarMinimize
import com.base.app.core.navigation.ShellConfig
import com.base.app.ui.LaunchHook
import com.base.app.ui.ShellConfigOverride
import com.base.app.ui.ShellObserver
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Launch extras for the benchmark journeys and tools/perf, read by dev builds only:
 *
 * - `perf.skipOnboarding=true` marks onboarding done;
 * - `perf.fakeSession=true` does that too and saves a synthetic session, so sign-in is skipped;
 * - `perf.screenResponse=0.30` sets the screen spring's response, in seconds, to compare candidates;
 * - `perf.barMinimize=OnScrollDown` makes the tab bar minimize while a list scrolls down.
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

    override suspend fun beforeStart(intent: Intent) {
        val fakeSession = intent.isOn(FAKE_SESSION)
        if (fakeSession || intent.isOn(SKIP_ONBOARDING)) settings.setOnboardingCompleted(true)
        if (fakeSession && tokens.accessToken() == null) tokens.save(SYNTHETIC_TOKEN, SYNTHETIC_TOKEN)
        screenResponse = intent.getStringExtra(SCREEN_RESPONSE)?.toFloatOrNull()?.takeIf { it > 0f }
        barMinimize = intent.getStringExtra(BAR_MINIMIZE)?.let { name -> BarMinimize.entries.firstOrNull { it.name == name } }
    }

    override fun adjust(config: ShellConfig): ShellConfig = config.copy(
        screenResponse = screenResponse ?: config.screenResponse,
        barMinimize = barMinimize ?: config.barMinimize,
    )

    private fun Intent.isOn(name: String): Boolean = getStringExtra(name).toBoolean()

    private companion object {
        const val SKIP_ONBOARDING = "perf.skipOnboarding"
        const val FAKE_SESSION = "perf.fakeSession"
        const val SCREEN_RESPONSE = "perf.screenResponse"
        const val BAR_MINIMIZE = "perf.barMinimize"
        const val SYNTHETIC_TOKEN = "perf-session"
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
