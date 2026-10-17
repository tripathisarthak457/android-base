package com.example.standardmodular.di

import com.example.standardmodular.BuildConfig
import com.example.standardmodular.feature.auth.api.AuthDestination
import com.example.standardmodular.feature.onboarding.api.OnboardingDestination
import com.example.standardmodular.feature.settings.api.SettingsAppInfo
import com.example.standardmodular.ui.AppDestinations
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

    @Provides
    @Singleton
    fun provideOnboardingDestination(): OnboardingDestination = OnboardingDestination(
        next = AppDestinations.afterOnboarding,
    )

    @Provides
    @Singleton
    fun provideAuthDestination(): AuthDestination = AuthDestination(
        next = AppDestinations.start,
    )
}
