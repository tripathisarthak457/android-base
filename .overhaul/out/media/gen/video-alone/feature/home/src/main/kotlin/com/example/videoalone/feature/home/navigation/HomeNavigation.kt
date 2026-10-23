package com.example.videoalone.feature.home.navigation

import com.example.videoalone.core.navigation.AppNavigator
import com.example.videoalone.core.navigation.NavGraphEntry
import com.example.videoalone.core.navigation.NavPane
import com.example.videoalone.core.navigation.navGraph
import com.example.videoalone.core.navigation.navKeys
import com.example.videoalone.feature.home.api.HomeDetailKey
import com.example.videoalone.feature.home.api.HomeListKey
import com.example.videoalone.feature.home.detail.HomeDetailRoute
import com.example.videoalone.feature.home.list.HomeListRoute
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet
import kotlinx.serialization.modules.SerializersModule

/**
 * How this feature joins the navigation graph: what renders each key, and how the keys are saved.
 * On a wide window the detail opens beside the list, because the two declare their panes.
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

    @Provides
    @IntoSet
    fun homeNavKeys(): SerializersModule = navKeys {
        subclass(HomeListKey::class, HomeListKey.serializer())
        subclass(HomeDetailKey::class, HomeDetailKey.serializer())
    }
}
