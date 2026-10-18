package com.example.wp04.base.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.example.wp04.base.BuildConfig
import com.example.wp04.base.feature.settings.SettingsAppInfo
import com.example.wp04.base.feature.auth.AuthDestination
import com.example.wp04.base.ui.AppDestinations

/** The handful of facts features need that only the application module can know. */
@Module
@InstallIn(SingletonComponent::class)
object FeatureBindingsModule {
    @Provides
    @Singleton
    fun provideSettingsAppInfo(): SettingsAppInfo = SettingsAppInfo(
        versionName = BuildConfig.VERSION_NAME,
    )

    @Provides
    @Singleton
    fun provideAuthDestination(): AuthDestination = AuthDestination(
        next = AppDestinations.start,
    )
}
