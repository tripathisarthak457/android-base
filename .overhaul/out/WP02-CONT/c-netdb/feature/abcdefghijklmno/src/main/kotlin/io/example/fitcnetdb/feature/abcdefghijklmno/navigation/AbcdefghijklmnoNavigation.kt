package io.example.fitcnetdb.feature.abcdefghijklmno.navigation

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
import io.example.fitcnetdb.feature.abcdefghijklmno.api.AbcdefghijklmnoDetailKey
import io.example.fitcnetdb.feature.abcdefghijklmno.api.AbcdefghijklmnoListKey
import io.example.fitcnetdb.feature.abcdefghijklmno.detail.AbcdefghijklmnoDetailRoute
import io.example.fitcnetdb.feature.abcdefghijklmno.list.AbcdefghijklmnoListRoute
import kotlinx.serialization.modules.SerializersModule

/**
 * How this feature joins the navigation graph: what renders each key, and how the keys are saved.
 * On a wide window the detail opens beside the list, because the two declare their panes.
 */
@Module
@InstallIn(ActivityRetainedComponent::class)
internal object AbcdefghijklmnoNavigation {

    @Provides
    @IntoSet
    fun abcdefghijklmnoNavGraph(navigator: AppNavigator): NavGraphEntry = navGraph {
        entry<AbcdefghijklmnoListKey>(pane = NavPane.List) {
            AbcdefghijklmnoListRoute(navigator = navigator)
        }
        entry<AbcdefghijklmnoDetailKey>(pane = NavPane.Detail) { key ->
            AbcdefghijklmnoDetailRoute(id = key.id, navigator = navigator)
        }
    }

    @Provides
    @IntoSet
    fun abcdefghijklmnoNavKeys(): SerializersModule = navKeys {
        subclass(AbcdefghijklmnoListKey::class, AbcdefghijklmnoListKey.serializer())
        subclass(AbcdefghijklmnoDetailKey::class, AbcdefghijklmnoDetailKey.serializer())
    }
}
