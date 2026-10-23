package io.example.fit.feature.abcdefghijklmnop.navigation

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
import io.example.fit.feature.abcdefghijklmnop.api.AbcdefghijklmnopDetailKey
import io.example.fit.feature.abcdefghijklmnop.api.AbcdefghijklmnopListKey
import io.example.fit.feature.abcdefghijklmnop.detail.AbcdefghijklmnopDetailRoute
import io.example.fit.feature.abcdefghijklmnop.list.AbcdefghijklmnopListRoute
import kotlinx.serialization.modules.SerializersModule

/**
 * How this feature joins the navigation graph: what renders each key, and how the keys are saved.
 * On a wide window the detail opens beside the list, because the two declare their panes.
 */
@Module
@InstallIn(ActivityRetainedComponent::class)
internal object AbcdefghijklmnopNavigation {

    @Provides
    @IntoSet
    fun abcdefghijklmnopNavGraph(navigator: AppNavigator): NavGraphEntry = navGraph {
        entry<AbcdefghijklmnopListKey>(pane = NavPane.List) {
            AbcdefghijklmnopListRoute(navigator = navigator)
        }
        entry<AbcdefghijklmnopDetailKey>(pane = NavPane.Detail) { key ->
            AbcdefghijklmnopDetailRoute(id = key.id, navigator = navigator)
        }
    }

    @Provides
    @IntoSet
    fun abcdefghijklmnopNavKeys(): SerializersModule = navKeys {
        subclass(AbcdefghijklmnopListKey::class, AbcdefghijklmnopListKey.serializer())
        subclass(AbcdefghijklmnopDetailKey::class, AbcdefghijklmnopDetailKey.serializer())
    }
}
