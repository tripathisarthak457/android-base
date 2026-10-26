package com.example.cmp.feature.profile.navigation

import com.example.cmp.core.navigation.NavGraphEntry
import com.example.cmp.core.navigation.navGraph
import com.example.cmp.feature.profile.ProfileRoute
import com.example.cmp.feature.profile.api.ProfileKey
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
