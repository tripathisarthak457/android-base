package io.example.fit.feature.abcdefghij.navigation

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
import io.example.fit.feature.abcdefghij.api.AbcdefghijDetailKey
import io.example.fit.feature.abcdefghij.api.AbcdefghijListKey
import io.example.fit.feature.abcdefghij.detail.AbcdefghijDetailRoute
import io.example.fit.feature.abcdefghij.list.AbcdefghijListRoute
import kotlinx.serialization.modules.SerializersModule

/**
 * How this feature joins the navigation graph: what renders each key, and how the keys are saved.
 * On a wide window the detail opens beside the list, because the two declare their panes.
 */
@Module
@InstallIn(ActivityRetainedComponent::class)
internal object AbcdefghijNavigation {

    @Provides
    @IntoSet
    fun abcdefghijNavGraph(navigator: AppNavigator): NavGraphEntry = navGraph {
        entry<AbcdefghijListKey>(pane = NavPane.List) {
            AbcdefghijListRoute(navigator = navigator)
        }
        entry<AbcdefghijDetailKey>(pane = NavPane.Detail) { key ->
            AbcdefghijDetailRoute(id = key.id, navigator = navigator)
        }
    }

    @Provides
    @IntoSet
    fun abcdefghijNavKeys(): SerializersModule = navKeys {
        subclass(AbcdefghijListKey::class, AbcdefghijListKey.serializer())
        subclass(AbcdefghijDetailKey::class, AbcdefghijDetailKey.serializer())
    }
}
