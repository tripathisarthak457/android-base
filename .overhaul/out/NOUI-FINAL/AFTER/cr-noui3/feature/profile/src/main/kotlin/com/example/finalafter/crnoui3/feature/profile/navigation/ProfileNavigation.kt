package com.example.finalafter.crnoui3.feature.profile.navigation

import com.example.finalafter.crnoui3.core.navigation.NavGraphEntry
import com.example.finalafter.crnoui3.core.navigation.navGraph
import com.example.finalafter.crnoui3.feature.profile.ProfileRoute
import com.example.finalafter.crnoui3.feature.profile.api.ProfileKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(ActivityRetainedComponent::class)
internal object ProfileNavigation {

    @Provides
    @IntoSet
    fun profileNavGraph(): NavGraphEntry = navGraph {
        entry<ProfileKey> { ProfileRoute() }
    }
}
