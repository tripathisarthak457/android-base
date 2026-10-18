package com.example.hud.fixture.feature.profile.di

import com.example.hud.fixture.core.navigation.NavGraphEntry
import com.example.hud.fixture.core.navigation.navGraph
import com.example.hud.fixture.core.navigation.navKeys
import com.example.hud.fixture.feature.profile.ProfileRoute
import com.example.hud.fixture.feature.profile.api.ProfileKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.serialization.modules.SerializersModule

@Module
@InstallIn(SingletonComponent::class)
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
