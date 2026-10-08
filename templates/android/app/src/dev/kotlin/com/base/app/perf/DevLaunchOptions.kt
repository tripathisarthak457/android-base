package com.base.app.perf

import android.content.Intent
import com.base.app.core.datastore.AppSettingsStore
import com.base.app.core.datastore.AuthTokenStore
import com.base.app.ui.LaunchHook
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Inject

/**
 * Launch extras that take a dev build straight to the tab shell without a backend, for the benchmark
 * journeys and tools/perf:
 *
 * - `perf.skipOnboarding=true` marks onboarding done;
 * - `perf.fakeSession=true` does that too and saves a synthetic session, so sign-in is skipped.
 *
 * The token is made up. A dev server that checks tokens answers 401 and the app signs out, so the
 * journeys rely on screens that load without a real session, as the sample feature's public API does.
 */
class DevLaunchOptions @Inject constructor(
    private val settings: AppSettingsStore,
    private val tokens: AuthTokenStore,
) : LaunchHook {

    override suspend fun beforeStart(intent: Intent) {
        val fakeSession = intent.isOn(FAKE_SESSION)
        if (fakeSession || intent.isOn(SKIP_ONBOARDING)) settings.setOnboardingCompleted(true)
        if (fakeSession && tokens.accessToken() == null) tokens.save(SYNTHETIC_TOKEN, SYNTHETIC_TOKEN)
    }

    private fun Intent.isOn(name: String): Boolean = getStringExtra(name).toBoolean()

    private companion object {
        const val SKIP_ONBOARDING = "perf.skipOnboarding"
        const val FAKE_SESSION = "perf.fakeSession"
        const val SYNTHETIC_TOKEN = "perf-session"
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface DevLaunchOptionsModule {

    @Binds
    @IntoSet
    fun devLaunchOptions(options: DevLaunchOptions): LaunchHook
}
