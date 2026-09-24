package com.base.app.feature.search.di

import com.base.app.core.navigation.AppNavigator
import com.base.app.core.navigation.NavGraphEntry
import com.base.app.core.navigation.NavPane
import com.base.app.core.navigation.navGraph
import com.base.app.core.navigation.navKeys
import com.base.app.feature.search.SearchKey
import com.base.app.feature.search.SearchResultKey
import com.base.app.feature.search.SearchResultRoute
import com.base.app.feature.search.SearchRoute
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
