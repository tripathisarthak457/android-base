package com.example.finalafter.crnoui.core.analytics.di

import com.example.finalafter.crnoui.core.analytics.CrashReporter
import com.example.finalafter.crnoui.core.analytics.firebase.CrashlyticsReporter
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
