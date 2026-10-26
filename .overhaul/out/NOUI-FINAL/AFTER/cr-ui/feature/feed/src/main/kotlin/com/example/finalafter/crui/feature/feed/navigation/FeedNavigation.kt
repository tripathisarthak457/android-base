package com.example.finalafter.crui.feature.feed.navigation

import com.example.finalafter.crui.core.navigation.NavGraphEntry
import com.example.finalafter.crui.core.navigation.navGraph
import com.example.finalafter.crui.feature.feed.FeedRoute
import com.example.finalafter.crui.feature.feed.api.FeedKey
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
