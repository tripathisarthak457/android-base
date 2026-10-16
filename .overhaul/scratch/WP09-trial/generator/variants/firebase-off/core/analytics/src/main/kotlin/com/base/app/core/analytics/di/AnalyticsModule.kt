package com.base.app.core.analytics.di

import com.base.app.core.analytics.AnalyticsTracker
import com.base.app.core.analytics.NoOpAnalyticsTracker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Analytics are not wired up in this project. Every `analytics.track(...)` call in feature code
 * still compiles and still runs — it simply goes nowhere.
 */
@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {

    @Provides
    @Singleton
    fun provideAnalyticsTracker(): AnalyticsTracker = NoOpAnalyticsTracker()
}
