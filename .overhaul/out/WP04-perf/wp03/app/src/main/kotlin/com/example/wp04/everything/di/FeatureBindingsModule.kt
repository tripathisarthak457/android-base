package com.example.wp04.everything.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.example.wp04.everything.BuildConfig
import com.example.wp04.everything.feature.settings.api.SettingsAppInfo
import com.example.wp04.everything.feature.onboarding.api.OnboardingDestination
import com.example.wp04.everything.feature.auth.api.AuthDestination
import com.example.wp04.everything.ui.AppDestinations

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
