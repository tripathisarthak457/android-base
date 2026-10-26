package com.example.finalafter.crnoui3.feature.onboarding.navigation

import com.example.finalafter.crnoui3.core.navigation.NavGraphEntry
import com.example.finalafter.crnoui3.core.navigation.navGraph
import com.example.finalafter.crnoui3.feature.onboarding.OnboardingRoute
import com.example.finalafter.crnoui3.feature.onboarding.api.OnboardingKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet

/**
 * Finishing writes the completed flag, and the app moves on to sign-in or the tabs by itself: they
 * are another host with their own stack, so Back never returns to the walkthrough.
 */
@Module
@InstallIn(ActivityRetainedComponent::class)
internal object OnboardingNavigation {

    @Provides
    @IntoSet
    fun onboardingNavGraph(): NavGraphEntry = navGraph {
        entry<OnboardingKey> { OnboardingRoute() }
    }
}
