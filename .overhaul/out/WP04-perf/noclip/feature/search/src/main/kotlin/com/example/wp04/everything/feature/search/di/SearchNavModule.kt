package com.example.wp04.everything.feature.search.di

import com.example.wp04.everything.core.navigation.AppNavigator
import com.example.wp04.everything.core.navigation.NavGraphEntry
import com.example.wp04.everything.core.navigation.NavPane
import com.example.wp04.everything.core.navigation.navGraph
import com.example.wp04.everything.core.navigation.navKeys
import com.example.wp04.everything.feature.search.SearchResultRoute
import com.example.wp04.everything.feature.search.SearchRoute
import com.example.wp04.everything.feature.search.api.SearchKey
import com.example.wp04.everything.feature.search.api.SearchResultKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet
import kotlinx.serialization.modules.SerializersModule

@Module
@InstallIn(ActivityRetainedComponent::class)
internal object SearchNavModule {

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

    @Provides
    @IntoSet
    fun searchNavKeys(): SerializersModule = navKeys {
        subclass(SearchKey::class, SearchKey.serializer())
        subclass(SearchResultKey::class, SearchResultKey.serializer())
    }
}
