package com.example.wp04.everything.feature.feed.di

import com.example.wp04.everything.core.navigation.NavGraphEntry
import com.example.wp04.everything.core.navigation.navGraph
import com.example.wp04.everything.core.navigation.navKeys
import com.example.wp04.everything.feature.feed.FeedRoute
import com.example.wp04.everything.feature.feed.api.FeedKey
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
