package io.example.fit.feature.abcdefghijklmnopqrst.navigation

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
import io.example.fit.feature.abcdefghijklmnopqrst.api.AbcdefghijklmnopqrstDetailKey
import io.example.fit.feature.abcdefghijklmnopqrst.api.AbcdefghijklmnopqrstListKey
import io.example.fit.feature.abcdefghijklmnopqrst.detail.AbcdefghijklmnopqrstDetailRoute
import io.example.fit.feature.abcdefghijklmnopqrst.list.AbcdefghijklmnopqrstListRoute
import kotlinx.serialization.modules.SerializersModule

/**
 * How this feature joins the navigation graph: what renders each key, and how the keys are saved.
 * On a wide window the detail opens beside the list, because the two declare their panes.
 */
@Module
@InstallIn(ActivityRetainedComponent::class)
internal object AbcdefghijklmnopqrstNavigation {

    @Provides
    @IntoSet
    fun abcdefghijklmnopqrstNavGraph(navigator: AppNavigator): NavGraphEntry = navGraph {
        entry<AbcdefghijklmnopqrstListKey>(pane = NavPane.List) {
            AbcdefghijklmnopqrstListRoute(navigator = navigator)
        }
        entry<AbcdefghijklmnopqrstDetailKey>(pane = NavPane.Detail) { key ->
            AbcdefghijklmnopqrstDetailRoute(id = key.id, navigator = navigator)
        }
    }

    @Provides
    @IntoSet
    fun abcdefghijklmnopqrstNavKeys(): SerializersModule = navKeys {
        subclass(AbcdefghijklmnopqrstListKey::class, AbcdefghijklmnopqrstListKey.serializer())
        subclass(AbcdefghijklmnopqrstDetailKey::class, AbcdefghijklmnopqrstDetailKey.serializer())
    }
}
