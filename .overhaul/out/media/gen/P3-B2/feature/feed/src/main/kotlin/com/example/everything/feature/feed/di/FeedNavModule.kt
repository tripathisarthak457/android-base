package com.example.everything.feature.feed.di

import com.example.everything.core.navigation.NavGraphEntry
import com.example.everything.core.navigation.navGraph
import com.example.everything.core.navigation.navKeys
import com.example.everything.feature.feed.FeedRoute
import com.example.everything.feature.feed.api.FeedKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet
import kotlinx.serialization.modules.SerializersModule

@Module
@InstallIn(ActivityRetainedComponent::class)
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
