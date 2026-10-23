package io.example.fit.feature.abcdefghijklmnopqrstu.navigation

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
import io.example.fit.feature.abcdefghijklmnopqrstu.api.AbcdefghijklmnopqrstuDetailKey
import io.example.fit.feature.abcdefghijklmnopqrstu.api.AbcdefghijklmnopqrstuListKey
import io.example.fit.feature.abcdefghijklmnopqrstu.detail.AbcdefghijklmnopqrstuDetailRoute
import io.example.fit.feature.abcdefghijklmnopqrstu.list.AbcdefghijklmnopqrstuListRoute
import kotlinx.serialization.modules.SerializersModule

/**
 * How this feature joins the navigation graph: what renders each key, and how the keys are saved.
 * On a wide window the detail opens beside the list, because the two declare their panes.
 */
@Module
@InstallIn(ActivityRetainedComponent::class)
internal object AbcdefghijklmnopqrstuNavigation {

    @Provides
    @IntoSet
    fun abcdefghijklmnopqrstuNavGraph(navigator: AppNavigator): NavGraphEntry = navGraph {
        entry<AbcdefghijklmnopqrstuListKey>(pane = NavPane.List) {
            AbcdefghijklmnopqrstuListRoute(navigator = navigator)
        }
        entry<AbcdefghijklmnopqrstuDetailKey>(pane = NavPane.Detail) { key ->
            AbcdefghijklmnopqrstuDetailRoute(id = key.id, navigator = navigator)
        }
    }

    @Provides
    @IntoSet
    fun abcdefghijklmnopqrstuNavKeys(): SerializersModule = navKeys {
        subclass(AbcdefghijklmnopqrstuListKey::class, AbcdefghijklmnopqrstuListKey.serializer())
        subclass(AbcdefghijklmnopqrstuDetailKey::class, AbcdefghijklmnopqrstuDetailKey.serializer())
    }
}
