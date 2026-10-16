// <opt:settings|onboarding|auth>
package com.base.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
// <opt:settings>
import com.base.app.BuildConfig
import com.base.app.feature.settings.SettingsAppInfo
// </opt:settings>
// <opt:onboarding>
import com.base.app.feature.onboarding.OnboardingDestination
// </opt:onboarding>
// <opt:auth>
import com.base.app.feature.auth.AuthDestination
// </opt:auth>
import com.base.app.ui.AppDestinations

/** The handful of facts features need that only the application module can know. */
@Module
@InstallIn(SingletonComponent::class)
object FeatureBindingsModule {

    // <opt:settings>
    @Provides
    @Singleton
    fun provideSettingsAppInfo(): SettingsAppInfo = SettingsAppInfo(
        versionName = BuildConfig.VERSION_NAME,
    )
    // </opt:settings>

    // <opt:onboarding>
    @Provides
    @Singleton
    fun provideOnboardingDestination(): OnboardingDestination = OnboardingDestination(
        next = AppDestinations.afterOnboarding,
    )
    // </opt:onboarding>

    // <opt:auth>
    @Provides
    @Singleton
    fun provideAuthDestination(): AuthDestination = AuthDestination(
        next = AppDestinations.start,
    )
    // </opt:auth>
}
// </opt:settings|onboarding|auth>
