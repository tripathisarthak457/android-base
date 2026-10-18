package com.example.hud.fixture.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.example.hud.fixture.BuildConfig
import com.example.hud.fixture.feature.settings.api.SettingsAppInfo
import com.example.hud.fixture.feature.onboarding.api.OnboardingDestination
import com.example.hud.fixture.feature.auth.api.AuthDestination
import com.example.hud.fixture.ui.AppDestinations

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
