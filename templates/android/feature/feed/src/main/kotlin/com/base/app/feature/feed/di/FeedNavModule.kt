package com.base.app.feature.feed.di

import com.base.app.core.navigation.NavGraphEntry
import com.base.app.core.navigation.navGraph
import com.base.app.core.navigation.navKeys
import com.base.app.feature.feed.FeedKey
import com.base.app.feature.feed.FeedRoute
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
