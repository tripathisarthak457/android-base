package com.base.app.core.analytics.di

import com.base.app.core.analytics.CrashReporter
import com.base.app.core.analytics.CrashlyticsReporter
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds the crash reporter. */
@Module
@InstallIn(SingletonComponent::class)
object CrashModule {

    @Provides
    @Singleton
    fun provideFirebaseCrashlytics(): FirebaseCrashlytics = FirebaseCrashlytics.getInstance()

    @Provides
    @Singleton
    fun provideCrashReporter(reporter: CrashlyticsReporter): CrashReporter = reporter
}
