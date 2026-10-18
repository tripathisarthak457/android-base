package com.example.wp04.base.feature.profile.di

import com.example.wp04.base.core.navigation.NavGraphEntry
import com.example.wp04.base.core.navigation.navGraph
import com.example.wp04.base.core.navigation.navKeys
import com.example.wp04.base.feature.profile.ProfileKey
import com.example.wp04.base.feature.profile.ProfileRoute
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.serialization.modules.SerializersModule

@Module
@InstallIn(SingletonComponent::class)
object ProfileNavModule {
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
