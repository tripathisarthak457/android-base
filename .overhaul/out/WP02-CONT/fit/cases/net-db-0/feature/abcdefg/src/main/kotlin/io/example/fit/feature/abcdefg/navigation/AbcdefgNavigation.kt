package io.example.fit.feature.abcdefg.navigation

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
import io.example.fit.feature.abcdefg.api.AbcdefgDetailKey
import io.example.fit.feature.abcdefg.api.AbcdefgListKey
import io.example.fit.feature.abcdefg.detail.AbcdefgDetailRoute
import io.example.fit.feature.abcdefg.list.AbcdefgListRoute
import kotlinx.serialization.modules.SerializersModule

/**
 * How this feature joins the navigation graph: what renders each key, and how the keys are saved.
 * On a wide window the detail opens beside the list, because the two declare their panes.
 */
@Module
@InstallIn(ActivityRetainedComponent::class)
internal object AbcdefgNavigation {

    @Provides
    @IntoSet
    fun abcdefgNavGraph(navigator: AppNavigator): NavGraphEntry = navGraph {
        entry<AbcdefgListKey>(pane = NavPane.List) {
            AbcdefgListRoute(navigator = navigator)
        }
        entry<AbcdefgDetailKey>(pane = NavPane.Detail) { key ->
            AbcdefgDetailRoute(id = key.id, navigator = navigator)
        }
    }

    @Provides
    @IntoSet
    fun abcdefgNavKeys(): SerializersModule = navKeys {
        subclass(AbcdefgListKey::class, AbcdefgListKey.serializer())
        subclass(AbcdefgDetailKey::class, AbcdefgDetailKey.serializer())
    }
}
