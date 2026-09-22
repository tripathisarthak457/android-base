package com.base.app.core.analytics.di

import com.base.app.core.analytics.CrashReporter
import com.base.app.core.analytics.NoOpCrashReporter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Crash reporting is not wired up in this project.
 *
 * `AppLogger.e(...)` still logs locally; it simply has nowhere to send a breadcrumb. Adding a
 * reporter later is a change to this one file.
 */
@Module
@InstallIn(SingletonComponent::class)
object CrashModule {

    @Provides
    @Singleton
    fun provideCrashReporter(): CrashReporter = NoOpCrashReporter()
}
