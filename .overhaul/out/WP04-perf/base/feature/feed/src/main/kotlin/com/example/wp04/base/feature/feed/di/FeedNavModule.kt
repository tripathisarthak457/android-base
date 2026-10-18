package com.example.wp04.base.feature.feed.di

import com.example.wp04.base.core.navigation.NavGraphEntry
import com.example.wp04.base.core.navigation.navGraph
import com.example.wp04.base.core.navigation.navKeys
import com.example.wp04.base.feature.feed.FeedKey
import com.example.wp04.base.feature.feed.FeedRoute
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.serialization.modules.SerializersModule

@Module
@InstallIn(SingletonComponent::class)
object FeedNavModule {
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
