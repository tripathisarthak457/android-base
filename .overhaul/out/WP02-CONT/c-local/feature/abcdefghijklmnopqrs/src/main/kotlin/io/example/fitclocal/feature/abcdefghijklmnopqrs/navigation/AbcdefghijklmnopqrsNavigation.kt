package io.example.fitclocal.feature.abcdefghijklmnopqrs.navigation

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet
import io.example.fitclocal.core.navigation.AppNavigator
import io.example.fitclocal.core.navigation.NavGraphEntry
import io.example.fitclocal.core.navigation.NavPane
import io.example.fitclocal.core.navigation.navGraph
import io.example.fitclocal.core.navigation.navKeys
import io.example.fitclocal.feature.abcdefghijklmnopqrs.api.AbcdefghijklmnopqrsDetailKey
import io.example.fitclocal.feature.abcdefghijklmnopqrs.api.AbcdefghijklmnopqrsListKey
import io.example.fitclocal.feature.abcdefghijklmnopqrs.detail.AbcdefghijklmnopqrsDetailRoute
import io.example.fitclocal.feature.abcdefghijklmnopqrs.list.AbcdefghijklmnopqrsListRoute
import kotlinx.serialization.modules.SerializersModule

/**
 * How this feature joins the navigation graph: what renders each key, and how the keys are saved.
 * On a wide window the detail opens beside the list, because the two declare their panes.
 */
@Module
@InstallIn(ActivityRetainedComponent::class)
internal object AbcdefghijklmnopqrsNavigation {

    @Provides
    @IntoSet
    fun abcdefghijklmnopqrsNavGraph(navigator: AppNavigator): NavGraphEntry = navGraph {
        entry<AbcdefghijklmnopqrsListKey>(pane = NavPane.List) {
            AbcdefghijklmnopqrsListRoute(navigator = navigator)
        }
        entry<AbcdefghijklmnopqrsDetailKey>(pane = NavPane.Detail) { key ->
            AbcdefghijklmnopqrsDetailRoute(id = key.id, navigator = navigator)
        }
    }

    @Provides
    @IntoSet
    fun abcdefghijklmnopqrsNavKeys(): SerializersModule = navKeys {
        subclass(AbcdefghijklmnopqrsListKey::class, AbcdefghijklmnopqrsListKey.serializer())
        subclass(AbcdefghijklmnopqrsDetailKey::class, AbcdefghijklmnopqrsDetailKey.serializer())
    }
}
