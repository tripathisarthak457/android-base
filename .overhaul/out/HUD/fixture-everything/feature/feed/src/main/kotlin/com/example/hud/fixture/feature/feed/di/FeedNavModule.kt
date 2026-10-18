package com.example.hud.fixture.feature.feed.di

import com.example.hud.fixture.core.navigation.NavGraphEntry
import com.example.hud.fixture.core.navigation.navGraph
import com.example.hud.fixture.core.navigation.navKeys
import com.example.hud.fixture.feature.feed.FeedRoute
import com.example.hud.fixture.feature.feed.api.FeedKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.serialization.modules.SerializersModule

@Module
@InstallIn(SingletonComponent::class)
internal object FeedNavModule {

    @Provides
    @IntoSet
    fun feedNavGraph(): NavGraphEntry = navGraph {
        entry<FeedKey> { FeedRoute() }
    }

    @Provides
    @IntoSet
    fun feedNavKeys(): SerializersModule = navKeys {
        subclass(FeedKey::class, FeedKey.serializer())
    }
}
