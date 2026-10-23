package io.example.fit.feature.abcdefghijkl.navigation

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet
import io.example.fit.core.navigation.AppNavigator
import io.example.fit.core.navigation.NavGraphEntry
import io.example.fit.core.navigation.NavPane
import io.example.fit.core.navigation.navGraph
import io.example.fit.core.navigation.navKeys
import io.example.fit.feature.abcdefghijkl.api.AbcdefghijklDetailKey
import io.example.fit.feature.abcdefghijkl.api.AbcdefghijklListKey
import io.example.fit.feature.abcdefghijkl.detail.AbcdefghijklDetailRoute
import io.example.fit.feature.abcdefghijkl.list.AbcdefghijklListRoute
import kotlinx.serialization.modules.SerializersModule

/**
 * How this feature joins the navigation graph: what renders each key, and how the keys are saved.
 * On a wide window the detail opens beside the list, because the two declare their panes.
 */
@Module
@InstallIn(ActivityRetainedComponent::class)
internal object AbcdefghijklNavigation {

    @Provides
    @IntoSet
    fun abcdefghijklNavGraph(navigator: AppNavigator): NavGraphEntry = navGraph {
        entry<AbcdefghijklListKey>(pane = NavPane.List) {
            AbcdefghijklListRoute(navigator = navigator)
        }
        entry<AbcdefghijklDetailKey>(pane = NavPane.Detail) { key ->
            AbcdefghijklDetailRoute(id = key.id, navigator = navigator)
        }
    }

    @Provides
    @IntoSet
    fun abcdefghijklNavKeys(): SerializersModule = navKeys {
        subclass(AbcdefghijklListKey::class, AbcdefghijklListKey.serializer())
        subclass(AbcdefghijklDetailKey::class, AbcdefghijklDetailKey.serializer())
    }
}
