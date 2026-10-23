package io.example.fitcnet.feature.abcdefghijklmnopqrstuvw.navigation

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet
import io.example.fitcnet.core.navigation.AppNavigator
import io.example.fitcnet.core.navigation.NavGraphEntry
import io.example.fitcnet.core.navigation.NavPane
import io.example.fitcnet.core.navigation.navGraph
import io.example.fitcnet.core.navigation.navKeys
import io.example.fitcnet.feature.abcdefghijklmnopqrstuvw.api.AbcdefghijklmnopqrstuvwDetailKey
import io.example.fitcnet.feature.abcdefghijklmnopqrstuvw.api.AbcdefghijklmnopqrstuvwListKey
import io.example.fitcnet.feature.abcdefghijklmnopqrstuvw.detail.AbcdefghijklmnopqrstuvwDetailRoute
import io.example.fitcnet.feature.abcdefghijklmnopqrstuvw.list.AbcdefghijklmnopqrstuvwListRoute
import kotlinx.serialization.modules.SerializersModule

/**
 * How this feature joins the navigation graph: what renders each key, and how the keys are saved.
 * On a wide window the detail opens beside the list, because the two declare their panes.
 */
@Module
@InstallIn(ActivityRetainedComponent::class)
internal object AbcdefghijklmnopqrstuvwNavigation {

    @Provides
    @IntoSet
    fun abcdefghijklmnopqrstuvwNavGraph(navigator: AppNavigator): NavGraphEntry = navGraph {
        entry<AbcdefghijklmnopqrstuvwListKey>(pane = NavPane.List) {
            AbcdefghijklmnopqrstuvwListRoute(navigator = navigator)
        }
        entry<AbcdefghijklmnopqrstuvwDetailKey>(pane = NavPane.Detail) { key ->
            AbcdefghijklmnopqrstuvwDetailRoute(id = key.id, navigator = navigator)
        }
    }

    @Provides
    @IntoSet
    fun abcdefghijklmnopqrstuvwNavKeys(): SerializersModule = navKeys {
        subclass(AbcdefghijklmnopqrstuvwListKey::class, AbcdefghijklmnopqrstuvwListKey.serializer())
        subclass(
            AbcdefghijklmnopqrstuvwDetailKey::class,
            AbcdefghijklmnopqrstuvwDetailKey.serializer(),
        )
    }
}
