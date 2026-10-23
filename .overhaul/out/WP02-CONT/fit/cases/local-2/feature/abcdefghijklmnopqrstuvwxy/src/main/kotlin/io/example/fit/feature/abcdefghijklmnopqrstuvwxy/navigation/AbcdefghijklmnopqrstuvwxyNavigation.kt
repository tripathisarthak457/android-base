package io.example.fit.feature.abcdefghijklmnopqrstuvwxy.navigation

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
import io.example.fit.feature.abcdefghijklmnopqrstuvwxy.api.AbcdefghijklmnopqrstuvwxyDetailKey
import io.example.fit.feature.abcdefghijklmnopqrstuvwxy.api.AbcdefghijklmnopqrstuvwxyListKey
import io.example.fit.feature.abcdefghijklmnopqrstuvwxy.detail.AbcdefghijklmnopqrstuvwxyDetailRoute
import io.example.fit.feature.abcdefghijklmnopqrstuvwxy.list.AbcdefghijklmnopqrstuvwxyListRoute
import kotlinx.serialization.modules.SerializersModule

/**
 * How this feature joins the navigation graph: what renders each key, and how the keys are saved.
 * On a wide window the detail opens beside the list, because the two declare their panes.
 */
@Module
@InstallIn(ActivityRetainedComponent::class)
internal object AbcdefghijklmnopqrstuvwxyNavigation {

    @Provides
    @IntoSet
    fun abcdefghijklmnopqrstuvwxyNavGraph(navigator: AppNavigator): NavGraphEntry = navGraph {
        entry<AbcdefghijklmnopqrstuvwxyListKey>(pane = NavPane.List) {
            AbcdefghijklmnopqrstuvwxyListRoute(navigator = navigator)
        }
        entry<AbcdefghijklmnopqrstuvwxyDetailKey>(pane = NavPane.Detail) { key ->
            AbcdefghijklmnopqrstuvwxyDetailRoute(id = key.id, navigator = navigator)
        }
    }

    @Provides
    @IntoSet
    fun abcdefghijklmnopqrstuvwxyNavKeys(): SerializersModule = navKeys {
        subclass(
            AbcdefghijklmnopqrstuvwxyListKey::class,
            AbcdefghijklmnopqrstuvwxyListKey.serializer(),
        )
        subclass(
            AbcdefghijklmnopqrstuvwxyDetailKey::class,
            AbcdefghijklmnopqrstuvwxyDetailKey.serializer(),
        )
    }
}
