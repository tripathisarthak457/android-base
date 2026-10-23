package io.example.fit.feature.abcd.navigation

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
import io.example.fit.feature.abcd.api.AbcdDetailKey
import io.example.fit.feature.abcd.api.AbcdListKey
import io.example.fit.feature.abcd.detail.AbcdDetailRoute
import io.example.fit.feature.abcd.list.AbcdListRoute
import kotlinx.serialization.modules.SerializersModule

/**
 * How this feature joins the navigation graph: what renders each key, and how the keys are saved.
 * On a wide window the detail opens beside the list, because the two declare their panes.
 */
@Module
@InstallIn(ActivityRetainedComponent::class)
internal object AbcdNavigation {

    @Provides
    @IntoSet
    fun abcdNavGraph(navigator: AppNavigator): NavGraphEntry = navGraph {
        entry<AbcdListKey>(pane = NavPane.List) {
            AbcdListRoute(navigator = navigator)
        }
        entry<AbcdDetailKey>(pane = NavPane.Detail) { key ->
            AbcdDetailRoute(id = key.id, navigator = navigator)
        }
    }

    @Provides
    @IntoSet
    fun abcdNavKeys(): SerializersModule = navKeys {
        subclass(AbcdListKey::class, AbcdListKey.serializer())
        subclass(AbcdDetailKey::class, AbcdDetailKey.serializer())
    }
}
