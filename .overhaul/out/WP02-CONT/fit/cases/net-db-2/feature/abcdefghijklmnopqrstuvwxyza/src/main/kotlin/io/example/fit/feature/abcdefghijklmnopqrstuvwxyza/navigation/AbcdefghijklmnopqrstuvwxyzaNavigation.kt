package io.example.fit.feature.abcdefghijklmnopqrstuvwxyza.navigation

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
import io.example.fit.feature.abcdefghijklmnopqrstuvwxyza.api.AbcdefghijklmnopqrstuvwxyzaDetailKey
import io.example.fit.feature.abcdefghijklmnopqrstuvwxyza.api.AbcdefghijklmnopqrstuvwxyzaListKey
import io.example.fit.feature.abcdefghijklmnopqrstuvwxyza.detail.AbcdefghijklmnopqrstuvwxyzaDetailRoute
import io.example.fit.feature.abcdefghijklmnopqrstuvwxyza.list.AbcdefghijklmnopqrstuvwxyzaListRoute
import kotlinx.serialization.modules.SerializersModule

/**
 * How this feature joins the navigation graph: what renders each key, and how the keys are saved.
 * On a wide window the detail opens beside the list, because the two declare their panes.
 */
@Module
@InstallIn(ActivityRetainedComponent::class)
internal object AbcdefghijklmnopqrstuvwxyzaNavigation {

    @Provides
    @IntoSet
    fun abcdefghijklmnopqrstuvwxyzaNavGraph(navigator: AppNavigator): NavGraphEntry = navGraph {
        entry<AbcdefghijklmnopqrstuvwxyzaListKey>(pane = NavPane.List) {
            AbcdefghijklmnopqrstuvwxyzaListRoute(navigator = navigator)
        }
        entry<AbcdefghijklmnopqrstuvwxyzaDetailKey>(pane = NavPane.Detail) { key ->
            AbcdefghijklmnopqrstuvwxyzaDetailRoute(id = key.id, navigator = navigator)
        }
    }

    @Provides
    @IntoSet
    fun abcdefghijklmnopqrstuvwxyzaNavKeys(): SerializersModule = navKeys {
        subclass(
            AbcdefghijklmnopqrstuvwxyzaListKey::class,
            AbcdefghijklmnopqrstuvwxyzaListKey.serializer(),
        )
        subclass(
            AbcdefghijklmnopqrstuvwxyzaDetailKey::class,
            AbcdefghijklmnopqrstuvwxyzaDetailKey.serializer(),
        )
    }
}
