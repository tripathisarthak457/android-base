package com.example.deskstandard.feature.onboarding

import com.example.deskstandard.core.navigation.NavGraphEntry
import com.example.deskstandard.core.navigation.navGraph
import com.example.deskstandard.core.navigation.navKeys
import com.example.deskstandard.feature.onboarding.api.OnboardingKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet
import kotlinx.serialization.modules.SerializersModule

/**
 * Finishing writes the completed flag, and the app moves on to sign-in or the tabs by itself: they
 * are another host with their own stack, so Back never returns to the walkthrough.
 */
@Module
@InstallIn(ActivityRetainedComponent::class)
internal object OnboardingNavModule {

    @Provides
    @IntoSet
    fun onboardingNavGraph(): NavGraphEntry = navGraph {
        entry<OnboardingKey> { OnboardingRoute() }
    }

    @Provides
    @IntoSet
    fun onboardingNavKeys(): SerializersModule = navKeys {
        subclass(OnboardingKey::class, OnboardingKey.serializer())
    }
}
