package io.example.fitcnetdb.feature.abcdefghi.navigation

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
import io.example.fitcnetdb.feature.abcdefghi.api.AbcdefghiDetailKey
import io.example.fitcnetdb.feature.abcdefghi.api.AbcdefghiListKey
import io.example.fitcnetdb.feature.abcdefghi.detail.AbcdefghiDetailRoute
import io.example.fitcnetdb.feature.abcdefghi.list.AbcdefghiListRoute
import kotlinx.serialization.modules.SerializersModule

/**
 * How this feature joins the navigation graph: what renders each key, and how the keys are saved.
 * On a wide window the detail opens beside the list, because the two declare their panes.
 */
@Module
@InstallIn(ActivityRetainedComponent::class)
internal object AbcdefghiNavigation {

    @Provides
    @IntoSet
    fun abcdefghiNavGraph(navigator: AppNavigator): NavGraphEntry = navGraph {
        entry<AbcdefghiListKey>(pane = NavPane.List) {
            AbcdefghiListRoute(navigator = navigator)
        }
        entry<AbcdefghiDetailKey>(pane = NavPane.Detail) { key ->
            AbcdefghiDetailRoute(id = key.id, navigator = navigator)
        }
    }

    @Provides
    @IntoSet
    fun abcdefghiNavKeys(): SerializersModule = navKeys {
        subclass(AbcdefghiListKey::class, AbcdefghiListKey.serializer())
        subclass(AbcdefghiDetailKey::class, AbcdefghiDetailKey.serializer())
    }
}
