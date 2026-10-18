package com.example.hud.fixture.core.analytics.di

import com.example.hud.fixture.core.analytics.AnalyticsTracker
import com.example.hud.fixture.core.analytics.CrashReporter
import com.example.hud.fixture.core.common.session.SessionScopedStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/** The next person's events and crashes are not filed under the last one's id. */
@Module
@InstallIn(SingletonComponent::class)
internal object AnalyticsSessionModule {

    @Provides
    @IntoSet
    fun provideAnalyticsUserClearing(analytics: AnalyticsTracker, crashes: CrashReporter): SessionScopedStore =
        SessionScopedStore {
            analytics.setUserId(null)
            crashes.setUserId(null)
        }
}
