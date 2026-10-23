package io.example.fit.feature.abcde.navigation

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
import io.example.fit.feature.abcde.api.AbcdeDetailKey
import io.example.fit.feature.abcde.api.AbcdeListKey
import io.example.fit.feature.abcde.detail.AbcdeDetailRoute
import io.example.fit.feature.abcde.list.AbcdeListRoute
import kotlinx.serialization.modules.SerializersModule

/**
 * How this feature joins the navigation graph: what renders each key, and how the keys are saved.
 * On a wide window the detail opens beside the list, because the two declare their panes.
 */
@Module
@InstallIn(ActivityRetainedComponent::class)
internal object AbcdeNavigation {

    @Provides
    @IntoSet
    fun abcdeNavGraph(navigator: AppNavigator): NavGraphEntry = navGraph {
        entry<AbcdeListKey>(pane = NavPane.List) {
            AbcdeListRoute(navigator = navigator)
        }
        entry<AbcdeDetailKey>(pane = NavPane.Detail) { key ->
            AbcdeDetailRoute(id = key.id, navigator = navigator)
        }
    }

    @Provides
    @IntoSet
    fun abcdeNavKeys(): SerializersModule = navKeys {
        subclass(AbcdeListKey::class, AbcdeListKey.serializer())
        subclass(AbcdeDetailKey::class, AbcdeDetailKey.serializer())
    }
}
