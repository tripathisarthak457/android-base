package io.example.fitclocal.feature.abc.navigation

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet
import io.example.fitclocal.core.navigation.AppNavigator
import io.example.fitclocal.core.navigation.NavGraphEntry
import io.example.fitclocal.core.navigation.NavPane
import io.example.fitclocal.core.navigation.navGraph
import io.example.fitclocal.core.navigation.navKeys
import io.example.fitclocal.feature.abc.api.AbcDetailKey
import io.example.fitclocal.feature.abc.api.AbcListKey
import io.example.fitclocal.feature.abc.detail.AbcDetailRoute
import io.example.fitclocal.feature.abc.list.AbcListRoute
import kotlinx.serialization.modules.SerializersModule

/**
 * How this feature joins the navigation graph: what renders each key, and how the keys are saved.
 * On a wide window the detail opens beside the list, because the two declare their panes.
 */
@Module
@InstallIn(ActivityRetainedComponent::class)
internal object AbcNavigation {

    @Provides
    @IntoSet
    fun abcNavGraph(navigator: AppNavigator): NavGraphEntry = navGraph {
        entry<AbcListKey>(pane = NavPane.List) {
            AbcListRoute(navigator = navigator)
        }
        entry<AbcDetailKey>(pane = NavPane.Detail) { key ->
            AbcDetailRoute(id = key.id, navigator = navigator)
        }
    }

    @Provides
    @IntoSet
    fun abcNavKeys(): SerializersModule = navKeys {
        subclass(AbcListKey::class, AbcListKey.serializer())
        subclass(AbcDetailKey::class, AbcDetailKey.serializer())
    }
}
