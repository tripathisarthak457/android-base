package com.example.hud.everything.feature.search.di

import com.example.hud.everything.core.navigation.AppNavigator
import com.example.hud.everything.core.navigation.NavGraphEntry
import com.example.hud.everything.core.navigation.NavPane
import com.example.hud.everything.core.navigation.navGraph
import com.example.hud.everything.core.navigation.navKeys
import com.example.hud.everything.feature.search.SearchResultRoute
import com.example.hud.everything.feature.search.SearchRoute
import com.example.hud.everything.feature.search.api.SearchKey
import com.example.hud.everything.feature.search.api.SearchResultKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.serialization.modules.SerializersModule

@Module
@InstallIn(SingletonComponent::class)
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
