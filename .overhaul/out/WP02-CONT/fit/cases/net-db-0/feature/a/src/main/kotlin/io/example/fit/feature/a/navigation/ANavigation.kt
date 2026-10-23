package io.example.fit.feature.a.navigation

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
import io.example.fit.feature.a.api.ADetailKey
import io.example.fit.feature.a.api.AListKey
import io.example.fit.feature.a.detail.ADetailRoute
import io.example.fit.feature.a.list.AListRoute
import kotlinx.serialization.modules.SerializersModule

/**
 * How this feature joins the navigation graph: what renders each key, and how the keys are saved.
 * On a wide window the detail opens beside the list, because the two declare their panes.
 */
@Module
@InstallIn(ActivityRetainedComponent::class)
internal object ANavigation {

    @Provides
    @IntoSet
    fun aNavGraph(navigator: AppNavigator): NavGraphEntry = navGraph {
        entry<AListKey>(pane = NavPane.List) {
            AListRoute(navigator = navigator)
        }
        entry<ADetailKey>(pane = NavPane.Detail) { key ->
            ADetailRoute(id = key.id, navigator = navigator)
        }
    }

    @Provides
    @IntoSet
    fun aNavKeys(): SerializersModule = navKeys {
        subclass(AListKey::class, AListKey.serializer())
        subclass(ADetailKey::class, ADetailKey.serializer())
    }
}
