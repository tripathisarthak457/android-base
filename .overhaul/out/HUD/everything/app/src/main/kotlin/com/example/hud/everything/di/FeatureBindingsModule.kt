package com.example.hud.everything.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.example.hud.everything.BuildConfig
import com.example.hud.everything.feature.settings.api.SettingsAppInfo
import com.example.hud.everything.ui.AppDestinations

/** The handful of facts features need that only the application module can know. */
@Module
@InstallIn(SingletonComponent::class)
object FeatureBindingsModule {
    @Provides
    @Singleton
    fun provideSettingsAppInfo(): SettingsAppInfo = SettingsAppInfo(
        versionName = BuildConfig.VERSION_NAME,
    )
}
