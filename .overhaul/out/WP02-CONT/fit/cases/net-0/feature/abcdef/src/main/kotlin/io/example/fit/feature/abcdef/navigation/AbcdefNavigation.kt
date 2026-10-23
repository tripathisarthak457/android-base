package io.example.fit.feature.abcdef.navigation

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
import io.example.fit.feature.abcdef.api.AbcdefDetailKey
import io.example.fit.feature.abcdef.api.AbcdefListKey
import io.example.fit.feature.abcdef.detail.AbcdefDetailRoute
import io.example.fit.feature.abcdef.list.AbcdefListRoute
import kotlinx.serialization.modules.SerializersModule

/**
 * How this feature joins the navigation graph: what renders each key, and how the keys are saved.
 * On a wide window the detail opens beside the list, because the two declare their panes.
 */
@Module
@InstallIn(ActivityRetainedComponent::class)
internal object AbcdefNavigation {

    @Provides
    @IntoSet
    fun abcdefNavGraph(navigator: AppNavigator): NavGraphEntry = navGraph {
        entry<AbcdefListKey>(pane = NavPane.List) {
            AbcdefListRoute(navigator = navigator)
        }
        entry<AbcdefDetailKey>(pane = NavPane.Detail) { key ->
            AbcdefDetailRoute(id = key.id, navigator = navigator)
        }
    }

    @Provides
    @IntoSet
    fun abcdefNavKeys(): SerializersModule = navKeys {
        subclass(AbcdefListKey::class, AbcdefListKey.serializer())
        subclass(AbcdefDetailKey::class, AbcdefDetailKey.serializer())
    }
}
