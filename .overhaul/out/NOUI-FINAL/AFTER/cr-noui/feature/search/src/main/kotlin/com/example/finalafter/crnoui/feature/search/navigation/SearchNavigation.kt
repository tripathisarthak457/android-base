package com.example.finalafter.crnoui.feature.search.navigation

import com.example.finalafter.crnoui.core.navigation.AppNavigator
import com.example.finalafter.crnoui.core.navigation.NavGraphEntry
import com.example.finalafter.crnoui.core.navigation.NavPane
import com.example.finalafter.crnoui.core.navigation.navGraph
import com.example.finalafter.crnoui.feature.search.SearchRoute
import com.example.finalafter.crnoui.feature.search.api.SearchKey
import com.example.finalafter.crnoui.feature.search.api.SearchResultKey
import com.example.finalafter.crnoui.feature.search.result.SearchResultRoute
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(ActivityRetainedComponent::class)
internal object SearchNavigation {

    @Provides
    @IntoSet
    fun searchNavGraph(navigator: AppNavigator): NavGraphEntry = navGraph {
        entry<SearchKey>(pane = NavPane.List) {
            SearchRoute(onOpenResult = { navigator.navigate(SearchResultKey(it)) })
        }
        entry<SearchResultKey>(pane = NavPane.Detail) { key ->
            SearchResultRoute(id = key.id, navigator = navigator)
        }
    }
}
