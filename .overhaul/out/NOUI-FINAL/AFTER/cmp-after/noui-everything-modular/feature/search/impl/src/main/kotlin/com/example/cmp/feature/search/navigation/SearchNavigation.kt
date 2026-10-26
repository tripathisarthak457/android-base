package com.example.cmp.feature.search.navigation

import com.example.cmp.core.navigation.AppNavigator
import com.example.cmp.core.navigation.NavGraphEntry
import com.example.cmp.core.navigation.NavPane
import com.example.cmp.core.navigation.navGraph
import com.example.cmp.feature.search.SearchRoute
import com.example.cmp.feature.search.api.SearchKey
import com.example.cmp.feature.search.api.SearchResultKey
import com.example.cmp.feature.search.result.SearchResultRoute
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
