package com.base.app.feature.onboarding.navigation

import com.base.app.core.navigation.NavGraphEntry
import com.base.app.core.navigation.navGraph
import com.base.app.feature.onboarding.OnboardingRoute
import com.base.app.feature.onboarding.api.OnboardingKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet

// <opt:ui_components>
/**
 * Finishing writes the completed flag, and the app moves on to sign-in or the tabs by itself: they
 * are another host with their own stack, so Back never returns to the walkthrough.
 */
// </opt:ui_components>
// <opt-else:ui_components>
//~ /**
//~  * Finishing writes the completed flag, and the app starts its back stack afresh by itself, so Back
//~  * never returns to the walkthrough.
//~  */
// </opt-else:ui_components>
@Module
@InstallIn(ActivityRetainedComponent::class)
internal object OnboardingNavigation {

    @Provides
    @IntoSet
    fun onboardingNavGraph(): NavGraphEntry = navGraph {
        entry<OnboardingKey> { OnboardingRoute() }
    }
}
