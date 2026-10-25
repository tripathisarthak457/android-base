package com.base.app.feature.feed.navigation

import com.base.app.core.navigation.NavGraphEntry
import com.base.app.core.navigation.navGraph
import com.base.app.feature.feed.FeedRoute
import com.base.app.feature.feed.api.FeedKey
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
