package com.example.wp04.everything.feature.profile.di

import com.example.wp04.everything.core.navigation.NavGraphEntry
import com.example.wp04.everything.core.navigation.navGraph
import com.example.wp04.everything.core.navigation.navKeys
import com.example.wp04.everything.feature.profile.ProfileRoute
import com.example.wp04.everything.feature.profile.api.ProfileKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet
import kotlinx.serialization.modules.SerializersModule

@Module
@InstallIn(ActivityRetainedComponent::class)
internal object ProfileNavModule {

    @Provides
    @IntoSet
    fun profileNavGraph(): NavGraphEntry = navGraph {
        entry<ProfileKey> { ProfileRoute() }
    }

    @Provides
    @IntoSet
    fun profileNavKeys(): SerializersModule = navKeys {
        subclass(ProfileKey::class, ProfileKey.serializer())
    }
}
