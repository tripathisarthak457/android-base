package io.example.fit.feature.abcdefghijklmnopqrstuvwxyzabcd.navigation

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
import io.example.fit.feature.abcdefghijklmnopqrstuvwxyzabcd.api.AbcdefghijklmnopqrstuvwxyzabcdDetailKey
import io.example.fit.feature.abcdefghijklmnopqrstuvwxyzabcd.api.AbcdefghijklmnopqrstuvwxyzabcdListKey
import io.example.fit.feature.abcdefghijklmnopqrstuvwxyzabcd.detail.AbcdefghijklmnopqrstuvwxyzabcdDetailRoute
import io.example.fit.feature.abcdefghijklmnopqrstuvwxyzabcd.list.AbcdefghijklmnopqrstuvwxyzabcdListRoute
import kotlinx.serialization.modules.SerializersModule

/**
 * How this feature joins the navigation graph: what renders each key, and how the keys are saved.
 * On a wide window the detail opens beside the list, because the two declare their panes.
 */
@Module
@InstallIn(ActivityRetainedComponent::class)
internal object AbcdefghijklmnopqrstuvwxyzabcdNavigation {

    @Provides
    @IntoSet
    fun abcdefghijklmnopqrstuvwxyzabcdNavGraph(navigator: AppNavigator): NavGraphEntry = navGraph {
        entry<AbcdefghijklmnopqrstuvwxyzabcdListKey>(pane = NavPane.List) {
            AbcdefghijklmnopqrstuvwxyzabcdListRoute(navigator = navigator)
        }
        entry<AbcdefghijklmnopqrstuvwxyzabcdDetailKey>(pane = NavPane.Detail) { key ->
            AbcdefghijklmnopqrstuvwxyzabcdDetailRoute(id = key.id, navigator = navigator)
        }
    }

    @Provides
    @IntoSet
    fun abcdefghijklmnopqrstuvwxyzabcdNavKeys(): SerializersModule = navKeys {
        subclass(
            AbcdefghijklmnopqrstuvwxyzabcdListKey::class,
            AbcdefghijklmnopqrstuvwxyzabcdListKey.serializer(),
        )
        subclass(
            AbcdefghijklmnopqrstuvwxyzabcdDetailKey::class,
            AbcdefghijklmnopqrstuvwxyzabcdDetailKey.serializer(),
        )
    }
}
