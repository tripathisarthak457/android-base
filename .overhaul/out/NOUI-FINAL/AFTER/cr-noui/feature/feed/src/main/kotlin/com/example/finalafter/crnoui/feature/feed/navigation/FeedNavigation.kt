package com.example.finalafter.crnoui.feature.feed.navigation

import com.example.finalafter.crnoui.core.navigation.NavGraphEntry
import com.example.finalafter.crnoui.core.navigation.navGraph
import com.example.finalafter.crnoui.feature.feed.FeedRoute
import com.example.finalafter.crnoui.feature.feed.api.FeedKey
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
