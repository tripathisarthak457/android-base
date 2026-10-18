package com.base.app.core.analytics.di

import com.base.app.core.analytics.CrashReporter
import com.base.app.core.analytics.NoOpCrashReporter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Crash reporting is not wired up in this project. */
@Module
@InstallIn(SingletonComponent::class)
object CrashModule {

    @Provides
    @Singleton
    fun provideCrashReporter(): CrashReporter = NoOpCrashReporter()
}
