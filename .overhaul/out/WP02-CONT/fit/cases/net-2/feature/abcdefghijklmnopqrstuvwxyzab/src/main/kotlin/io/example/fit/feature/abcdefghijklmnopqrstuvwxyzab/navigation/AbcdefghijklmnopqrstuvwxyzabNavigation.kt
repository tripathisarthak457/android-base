package io.example.fit.feature.abcdefghijklmnopqrstuvwxyzab.navigation

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
import io.example.fit.feature.abcdefghijklmnopqrstuvwxyzab.api.AbcdefghijklmnopqrstuvwxyzabDetailKey
import io.example.fit.feature.abcdefghijklmnopqrstuvwxyzab.api.AbcdefghijklmnopqrstuvwxyzabListKey
import io.example.fit.feature.abcdefghijklmnopqrstuvwxyzab.detail.AbcdefghijklmnopqrstuvwxyzabDetailRoute
import io.example.fit.feature.abcdefghijklmnopqrstuvwxyzab.list.AbcdefghijklmnopqrstuvwxyzabListRoute
import kotlinx.serialization.modules.SerializersModule

/**
 * How this feature joins the navigation graph: what renders each key, and how the keys are saved.
 * On a wide window the detail opens beside the list, because the two declare their panes.
 */
@Module
@InstallIn(ActivityRetainedComponent::class)
internal object AbcdefghijklmnopqrstuvwxyzabNavigation {

    @Provides
    @IntoSet
    fun abcdefghijklmnopqrstuvwxyzabNavGraph(navigator: AppNavigator): NavGraphEntry = navGraph {
        entry<AbcdefghijklmnopqrstuvwxyzabListKey>(pane = NavPane.List) {
            AbcdefghijklmnopqrstuvwxyzabListRoute(navigator = navigator)
        }
        entry<AbcdefghijklmnopqrstuvwxyzabDetailKey>(pane = NavPane.Detail) { key ->
            AbcdefghijklmnopqrstuvwxyzabDetailRoute(id = key.id, navigator = navigator)
        }
    }

    @Provides
    @IntoSet
    fun abcdefghijklmnopqrstuvwxyzabNavKeys(): SerializersModule = navKeys {
        subclass(
            AbcdefghijklmnopqrstuvwxyzabListKey::class,
            AbcdefghijklmnopqrstuvwxyzabListKey.serializer(),
        )
        subclass(
            AbcdefghijklmnopqrstuvwxyzabDetailKey::class,
            AbcdefghijklmnopqrstuvwxyzabDetailKey.serializer(),
        )
    }
}
