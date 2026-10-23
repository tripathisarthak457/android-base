package io.example.fit.feature.abcdefghijklmnopqrstuvwx.navigation

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
import io.example.fit.feature.abcdefghijklmnopqrstuvwx.api.AbcdefghijklmnopqrstuvwxDetailKey
import io.example.fit.feature.abcdefghijklmnopqrstuvwx.api.AbcdefghijklmnopqrstuvwxListKey
import io.example.fit.feature.abcdefghijklmnopqrstuvwx.detail.AbcdefghijklmnopqrstuvwxDetailRoute
import io.example.fit.feature.abcdefghijklmnopqrstuvwx.list.AbcdefghijklmnopqrstuvwxListRoute
import kotlinx.serialization.modules.SerializersModule

/**
 * How this feature joins the navigation graph: what renders each key, and how the keys are saved.
 * On a wide window the detail opens beside the list, because the two declare their panes.
 */
@Module
@InstallIn(ActivityRetainedComponent::class)
internal object AbcdefghijklmnopqrstuvwxNavigation {

    @Provides
    @IntoSet
    fun abcdefghijklmnopqrstuvwxNavGraph(navigator: AppNavigator): NavGraphEntry = navGraph {
        entry<AbcdefghijklmnopqrstuvwxListKey>(pane = NavPane.List) {
            AbcdefghijklmnopqrstuvwxListRoute(navigator = navigator)
        }
        entry<AbcdefghijklmnopqrstuvwxDetailKey>(pane = NavPane.Detail) { key ->
            AbcdefghijklmnopqrstuvwxDetailRoute(id = key.id, navigator = navigator)
        }
    }

    @Provides
    @IntoSet
    fun abcdefghijklmnopqrstuvwxNavKeys(): SerializersModule = navKeys {
        subclass(
            AbcdefghijklmnopqrstuvwxListKey::class,
            AbcdefghijklmnopqrstuvwxListKey.serializer(),
        )
        subclass(
            AbcdefghijklmnopqrstuvwxDetailKey::class,
            AbcdefghijklmnopqrstuvwxDetailKey.serializer(),
        )
    }
}
