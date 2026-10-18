package com.example.wp04.everything.di

import com.example.wp04.everything.BuildConfig
import com.example.wp04.everything.feature.settings.api.SettingsAppInfo
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

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
