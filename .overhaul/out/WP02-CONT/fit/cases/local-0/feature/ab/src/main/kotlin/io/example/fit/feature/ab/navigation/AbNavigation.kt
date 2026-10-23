package io.example.fit.feature.ab.navigation

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
import io.example.fit.feature.ab.api.AbDetailKey
import io.example.fit.feature.ab.api.AbListKey
import io.example.fit.feature.ab.detail.AbDetailRoute
import io.example.fit.feature.ab.list.AbListRoute
import kotlinx.serialization.modules.SerializersModule

/**
 * How this feature joins the navigation graph: what renders each key, and how the keys are saved.
 * On a wide window the detail opens beside the list, because the two declare their panes.
 */
@Module
@InstallIn(ActivityRetainedComponent::class)
internal object AbNavigation {

    @Provides
    @IntoSet
    fun abNavGraph(navigator: AppNavigator): NavGraphEntry = navGraph {
        entry<AbListKey>(pane = NavPane.List) {
            AbListRoute(navigator = navigator)
        }
        entry<AbDetailKey>(pane = NavPane.Detail) { key ->
            AbDetailRoute(id = key.id, navigator = navigator)
        }
    }

    @Provides
    @IntoSet
    fun abNavKeys(): SerializersModule = navKeys {
        subclass(AbListKey::class, AbListKey.serializer())
        subclass(AbDetailKey::class, AbDetailKey.serializer())
    }
}
