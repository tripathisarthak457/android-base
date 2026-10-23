package io.example.fit.feature.abcdefghijklmnopq.navigation

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
import io.example.fit.feature.abcdefghijklmnopq.api.AbcdefghijklmnopqDetailKey
import io.example.fit.feature.abcdefghijklmnopq.api.AbcdefghijklmnopqListKey
import io.example.fit.feature.abcdefghijklmnopq.detail.AbcdefghijklmnopqDetailRoute
import io.example.fit.feature.abcdefghijklmnopq.list.AbcdefghijklmnopqListRoute
import kotlinx.serialization.modules.SerializersModule

/**
 * How this feature joins the navigation graph: what renders each key, and how the keys are saved.
 * On a wide window the detail opens beside the list, because the two declare their panes.
 */
@Module
@InstallIn(ActivityRetainedComponent::class)
internal object AbcdefghijklmnopqNavigation {

    @Provides
    @IntoSet
    fun abcdefghijklmnopqNavGraph(navigator: AppNavigator): NavGraphEntry = navGraph {
        entry<AbcdefghijklmnopqListKey>(pane = NavPane.List) {
            AbcdefghijklmnopqListRoute(navigator = navigator)
        }
        entry<AbcdefghijklmnopqDetailKey>(pane = NavPane.Detail) { key ->
            AbcdefghijklmnopqDetailRoute(id = key.id, navigator = navigator)
        }
    }

    @Provides
    @IntoSet
    fun abcdefghijklmnopqNavKeys(): SerializersModule = navKeys {
        subclass(AbcdefghijklmnopqListKey::class, AbcdefghijklmnopqListKey.serializer())
        subclass(AbcdefghijklmnopqDetailKey::class, AbcdefghijklmnopqDetailKey.serializer())
    }
}
