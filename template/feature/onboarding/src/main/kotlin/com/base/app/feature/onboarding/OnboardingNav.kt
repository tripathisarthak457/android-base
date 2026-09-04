package com.base.app.feature.onboarding

import com.base.app.core.navigation.AppNavKey
import com.base.app.core.navigation.AppNavigator
import com.base.app.core.navigation.NavGraphEntry
import com.base.app.core.navigation.navGraph
import com.base.app.core.navigation.navKeys
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule

@Serializable
data object OnboardingKey : AppNavKey

/**
 * Where the app goes once onboarding is finished.
 *
 * Supplied by the application module: onboarding cannot name the home screen without depending on
 * the feature that owns it, and a `:feature:` depending on another `:feature:` is the edge that
 * turns a module graph into a knot.
 */
data class OnboardingDestination(val next: AppNavKey)

/**
 * `resetTo` rather than `navigate`, so Back from the first real screen leaves the app instead of
 * returning to a walkthrough the user has already completed.
 */
@Module
@InstallIn(SingletonComponent::class)
object OnboardingNavModule {

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
