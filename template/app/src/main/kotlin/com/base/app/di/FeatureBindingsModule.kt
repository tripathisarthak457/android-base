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

/**
 * The handful of facts features need that only the application module can know.
 *
 * A feature that needs the app's version, or the name of a screen another feature owns, has two
 * options: depend on the module that has it — which is the edge that turns a module graph into a
 * knot — or declare a type and let the composition root fill it in. This is the second, and it is
 * the reason `:feature:onboarding` can hand control to the home screen without knowing it exists.
 *
 * Each binding is its own type rather than a qualified `String`, because Hilt matches on type:
 * two unqualified `String` providers in one graph is a compile error whose message names neither
 * of them.
 */
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
