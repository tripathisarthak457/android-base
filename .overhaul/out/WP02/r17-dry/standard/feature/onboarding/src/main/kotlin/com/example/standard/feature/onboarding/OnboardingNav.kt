package com.example.standard.feature.onboarding

import com.example.standard.core.navigation.AppNavigator
import com.example.standard.core.navigation.NavGraphEntry
import com.example.standard.core.navigation.navGraph
import com.example.standard.core.navigation.navKeys
import com.example.standard.feature.onboarding.api.OnboardingDestination
import com.example.standard.feature.onboarding.api.OnboardingKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.serialization.modules.SerializersModule

/**
 * `resetTo` rather than `navigate`, so Back from the first real screen leaves the app instead of
 * returning to a walkthrough the user has already completed.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object OnboardingNavModule {

    @Provides
    @IntoSet
    fun onboardingNavGraph(
        navigator: AppNavigator,
        destination: OnboardingDestination,
    ): NavGraphEntry = navGraph {
        entry<OnboardingKey> {
            OnboardingRoute(onFinished = { navigator.resetTo(destination.next) })
        }
    }

    @Provides
    @IntoSet
    fun onboardingNavKeys(): SerializersModule = navKeys {
        subclass(OnboardingKey::class, OnboardingKey.serializer())
    }
}
