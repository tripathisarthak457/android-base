package io.example.fit.feature.abcdefgh.navigation

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
import io.example.fit.feature.abcdefgh.api.AbcdefghDetailKey
import io.example.fit.feature.abcdefgh.api.AbcdefghListKey
import io.example.fit.feature.abcdefgh.detail.AbcdefghDetailRoute
import io.example.fit.feature.abcdefgh.list.AbcdefghListRoute
import kotlinx.serialization.modules.SerializersModule

/**
 * How this feature joins the navigation graph: what renders each key, and how the keys are saved.
 * On a wide window the detail opens beside the list, because the two declare their panes.
 */
@Module
@InstallIn(ActivityRetainedComponent::class)
internal object AbcdefghNavigation {

    @Provides
    @IntoSet
    fun abcdefghNavGraph(navigator: AppNavigator): NavGraphEntry = navGraph {
        entry<AbcdefghListKey>(pane = NavPane.List) {
            AbcdefghListRoute(navigator = navigator)
        }
        entry<AbcdefghDetailKey>(pane = NavPane.Detail) { key ->
            AbcdefghDetailRoute(id = key.id, navigator = navigator)
        }
    }

    @Provides
    @IntoSet
    fun abcdefghNavKeys(): SerializersModule = navKeys {
        subclass(AbcdefghListKey::class, AbcdefghListKey.serializer())
        subclass(AbcdefghDetailKey::class, AbcdefghDetailKey.serializer())
    }
}
