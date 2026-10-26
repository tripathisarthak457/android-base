package com.example.cmp.feature.home.navigation

import com.example.cmp.core.navigation.AppNavigator
import com.example.cmp.core.navigation.NavGraphEntry
import com.example.cmp.core.navigation.NavPane
import com.example.cmp.core.navigation.navGraph
import com.example.cmp.feature.home.api.HomeDetailKey
import com.example.cmp.feature.home.api.HomeListKey
import com.example.cmp.feature.home.detail.HomeDetailRoute
import com.example.cmp.feature.home.list.HomeListRoute
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet

/**
 * How this feature joins the navigation graph: what renders each key, which also lets the back
 * stack save the key. On a wide window the detail opens beside the list, because the two declare
 * their panes.
 */
@Module
@InstallIn(ActivityRetainedComponent::class)
internal object HomeNavigation {

    @Provides
    @IntoSet
    fun homeNavGraph(navigator: AppNavigator): NavGraphEntry = navGraph {
        entry<HomeListKey>(pane = NavPane.List) {
            HomeListRoute(navigator = navigator)
        }
        entry<HomeDetailKey>(pane = NavPane.Detail) { key ->
            HomeDetailRoute(id = key.id, navigator = navigator)
        }
    }
}
