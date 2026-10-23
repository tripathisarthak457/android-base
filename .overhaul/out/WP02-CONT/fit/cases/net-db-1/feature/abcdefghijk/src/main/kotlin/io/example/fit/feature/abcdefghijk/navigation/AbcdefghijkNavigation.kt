package io.example.fit.feature.abcdefghijk.navigation

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
import io.example.fit.feature.abcdefghijk.api.AbcdefghijkDetailKey
import io.example.fit.feature.abcdefghijk.api.AbcdefghijkListKey
import io.example.fit.feature.abcdefghijk.detail.AbcdefghijkDetailRoute
import io.example.fit.feature.abcdefghijk.list.AbcdefghijkListRoute
import kotlinx.serialization.modules.SerializersModule

/**
 * How this feature joins the navigation graph: what renders each key, and how the keys are saved.
 * On a wide window the detail opens beside the list, because the two declare their panes.
 */
@Module
@InstallIn(ActivityRetainedComponent::class)
internal object AbcdefghijkNavigation {

    @Provides
    @IntoSet
    fun abcdefghijkNavGraph(navigator: AppNavigator): NavGraphEntry = navGraph {
        entry<AbcdefghijkListKey>(pane = NavPane.List) {
            AbcdefghijkListRoute(navigator = navigator)
        }
        entry<AbcdefghijkDetailKey>(pane = NavPane.Detail) { key ->
            AbcdefghijkDetailRoute(id = key.id, navigator = navigator)
        }
    }

    @Provides
    @IntoSet
    fun abcdefghijkNavKeys(): SerializersModule = navKeys {
        subclass(AbcdefghijkListKey::class, AbcdefghijkListKey.serializer())
        subclass(AbcdefghijkDetailKey::class, AbcdefghijkDetailKey.serializer())
    }
}
