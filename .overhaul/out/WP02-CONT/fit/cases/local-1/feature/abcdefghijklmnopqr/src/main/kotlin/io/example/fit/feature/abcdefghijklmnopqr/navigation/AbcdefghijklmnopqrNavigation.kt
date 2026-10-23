package io.example.fit.feature.abcdefghijklmnopqr.navigation

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
import io.example.fit.feature.abcdefghijklmnopqr.api.AbcdefghijklmnopqrDetailKey
import io.example.fit.feature.abcdefghijklmnopqr.api.AbcdefghijklmnopqrListKey
import io.example.fit.feature.abcdefghijklmnopqr.detail.AbcdefghijklmnopqrDetailRoute
import io.example.fit.feature.abcdefghijklmnopqr.list.AbcdefghijklmnopqrListRoute
import kotlinx.serialization.modules.SerializersModule

/**
 * How this feature joins the navigation graph: what renders each key, and how the keys are saved.
 * On a wide window the detail opens beside the list, because the two declare their panes.
 */
@Module
@InstallIn(ActivityRetainedComponent::class)
internal object AbcdefghijklmnopqrNavigation {

    @Provides
    @IntoSet
    fun abcdefghijklmnopqrNavGraph(navigator: AppNavigator): NavGraphEntry = navGraph {
        entry<AbcdefghijklmnopqrListKey>(pane = NavPane.List) {
            AbcdefghijklmnopqrListRoute(navigator = navigator)
        }
        entry<AbcdefghijklmnopqrDetailKey>(pane = NavPane.Detail) { key ->
            AbcdefghijklmnopqrDetailRoute(id = key.id, navigator = navigator)
        }
    }

    @Provides
    @IntoSet
    fun abcdefghijklmnopqrNavKeys(): SerializersModule = navKeys {
        subclass(AbcdefghijklmnopqrListKey::class, AbcdefghijklmnopqrListKey.serializer())
        subclass(AbcdefghijklmnopqrDetailKey::class, AbcdefghijklmnopqrDetailKey.serializer())
    }
}
