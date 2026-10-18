package com.example.wp04.base.feature.search.di

import com.example.wp04.base.core.navigation.AppNavigator
import com.example.wp04.base.core.navigation.NavGraphEntry
import com.example.wp04.base.core.navigation.NavPane
import com.example.wp04.base.core.navigation.navGraph
import com.example.wp04.base.core.navigation.navKeys
import com.example.wp04.base.feature.search.SearchKey
import com.example.wp04.base.feature.search.SearchResultKey
import com.example.wp04.base.feature.search.SearchResultRoute
import com.example.wp04.base.feature.search.SearchRoute
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.serialization.modules.SerializersModule

@Module
@InstallIn(SingletonComponent::class)
object SearchNavModule {
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
