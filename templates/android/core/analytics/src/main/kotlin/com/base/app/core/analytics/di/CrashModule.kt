package com.base.app.core.analytics.di

import com.base.app.core.analytics.CrashReporter
// <opt:!crashlytics>import com.base.app.core.analytics.NoOpCrashReporter
// <opt:crashlytics>
import com.base.app.core.analytics.firebase.CrashlyticsReporter
import com.google.firebase.crashlytics.FirebaseCrashlytics
// </opt:crashlytics>
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds the crash reporter. */
@Module
@InstallIn(SingletonComponent::class)
object CrashModule {
    // <opt:crashlytics>

    @Provides
    @Singleton
    fun provideFirebaseCrashlytics(): FirebaseCrashlytics = FirebaseCrashlytics.getInstance()

    @Provides
    @Singleton
    fun provideCrashReporter(reporter: CrashlyticsReporter): CrashReporter = reporter
    // </opt:crashlytics>
    // <opt-else:crashlytics>
    //~
    //~     @Provides
    //~     @Singleton
    //~     fun provideCrashReporter(): CrashReporter = NoOpCrashReporter()
    // </opt-else:crashlytics>
}
