package com.example.hud.fixture.core.analytics.di

import com.example.hud.fixture.core.analytics.CrashReporter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.example.hud.fixture.core.analytics.firebase.CrashlyticsReporter
import com.google.firebase.crashlytics.FirebaseCrashlytics

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
