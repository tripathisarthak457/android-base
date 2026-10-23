package io.example.fit.feature.abcdefghijklmnopqrstuvwxyz.navigation

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
import io.example.fit.feature.abcdefghijklmnopqrstuvwxyz.api.AbcdefghijklmnopqrstuvwxyzDetailKey
import io.example.fit.feature.abcdefghijklmnopqrstuvwxyz.api.AbcdefghijklmnopqrstuvwxyzListKey
import io.example.fit.feature.abcdefghijklmnopqrstuvwxyz.detail.AbcdefghijklmnopqrstuvwxyzDetailRoute
import io.example.fit.feature.abcdefghijklmnopqrstuvwxyz.list.AbcdefghijklmnopqrstuvwxyzListRoute
import kotlinx.serialization.modules.SerializersModule

/**
 * How this feature joins the navigation graph: what renders each key, and how the keys are saved.
 * On a wide window the detail opens beside the list, because the two declare their panes.
 */
@Module
@InstallIn(ActivityRetainedComponent::class)
internal object AbcdefghijklmnopqrstuvwxyzNavigation {

    @Provides
    @IntoSet
    fun abcdefghijklmnopqrstuvwxyzNavGraph(navigator: AppNavigator): NavGraphEntry = navGraph {
        entry<AbcdefghijklmnopqrstuvwxyzListKey>(pane = NavPane.List) {
            AbcdefghijklmnopqrstuvwxyzListRoute(navigator = navigator)
        }
        entry<AbcdefghijklmnopqrstuvwxyzDetailKey>(pane = NavPane.Detail) { key ->
            AbcdefghijklmnopqrstuvwxyzDetailRoute(id = key.id, navigator = navigator)
        }
    }

    @Provides
    @IntoSet
    fun abcdefghijklmnopqrstuvwxyzNavKeys(): SerializersModule = navKeys {
        subclass(
            AbcdefghijklmnopqrstuvwxyzListKey::class,
            AbcdefghijklmnopqrstuvwxyzListKey.serializer(),
        )
        subclass(
            AbcdefghijklmnopqrstuvwxyzDetailKey::class,
            AbcdefghijklmnopqrstuvwxyzDetailKey.serializer(),
        )
    }
}
