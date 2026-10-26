package com.example.cmp.feature.feed.navigation

import com.example.cmp.core.navigation.NavGraphEntry
import com.example.cmp.core.navigation.navGraph
import com.example.cmp.feature.feed.FeedRoute
import com.example.cmp.feature.feed.api.FeedKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(ActivityRetainedComponent::class)
internal object FeedNavigation {

    @Provides
    @IntoSet
    fun feedNavGraph(): NavGraphEntry = navGraph {
        entry<FeedKey> { FeedRoute() }
    }
}
