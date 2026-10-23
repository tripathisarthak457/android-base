package io.example.fitcnetdb.feature.abcdefghijklmnopqrstuvw.navigation

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet
import io.example.fitcnetdb.core.navigation.AppNavigator
import io.example.fitcnetdb.core.navigation.NavGraphEntry
import io.example.fitcnetdb.core.navigation.NavPane
import io.example.fitcnetdb.core.navigation.navGraph
import io.example.fitcnetdb.core.navigation.navKeys
import io.example.fitcnetdb.feature.abcdefghijklmnopqrstuvw.api.AbcdefghijklmnopqrstuvwDetailKey
import io.example.fitcnetdb.feature.abcdefghijklmnopqrstuvw.api.AbcdefghijklmnopqrstuvwListKey
import io.example.fitcnetdb.feature.abcdefghijklmnopqrstuvw.detail.AbcdefghijklmnopqrstuvwDetailRoute
import io.example.fitcnetdb.feature.abcdefghijklmnopqrstuvw.list.AbcdefghijklmnopqrstuvwListRoute
import kotlinx.serialization.modules.SerializersModule

/**
 * How this feature joins the navigation graph: what renders each key, and how the keys are saved.
 * On a wide window the detail opens beside the list, because the two declare their panes.
 */
@Module
@InstallIn(ActivityRetainedComponent::class)
internal object AbcdefghijklmnopqrstuvwNavigation {

    @Provides
    @IntoSet
    fun abcdefghijklmnopqrstuvwNavGraph(navigator: AppNavigator): NavGraphEntry = navGraph {
        entry<AbcdefghijklmnopqrstuvwListKey>(pane = NavPane.List) {
            AbcdefghijklmnopqrstuvwListRoute(navigator = navigator)
        }
        entry<AbcdefghijklmnopqrstuvwDetailKey>(pane = NavPane.Detail) { key ->
            AbcdefghijklmnopqrstuvwDetailRoute(id = key.id, navigator = navigator)
        }
    }

    @Provides
    @IntoSet
    fun abcdefghijklmnopqrstuvwNavKeys(): SerializersModule = navKeys {
        subclass(AbcdefghijklmnopqrstuvwListKey::class, AbcdefghijklmnopqrstuvwListKey.serializer())
        subclass(
            AbcdefghijklmnopqrstuvwDetailKey::class,
            AbcdefghijklmnopqrstuvwDetailKey.serializer(),
        )
    }
}
