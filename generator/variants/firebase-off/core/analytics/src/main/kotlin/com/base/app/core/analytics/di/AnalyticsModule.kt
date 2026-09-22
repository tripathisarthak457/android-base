package com.base.app.core.analytics.di

import com.base.app.core.analytics.AnalyticsTracker
import com.base.app.core.analytics.NoOpAnalyticsTracker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Analytics are not wired up in this project.
 *
 * Every `analytics.track(...)` call in feature code still compiles and still runs — it simply
 * goes nowhere. That is deliberate: adding a vendor later is a change to this one file, not a
 * pass through every screen to add instrumentation that should have been there all along.
 */
@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {

    @Provides
    @Singleton
    fun provideAnalyticsTracker(): AnalyticsTracker = NoOpAnalyticsTracker()
}
