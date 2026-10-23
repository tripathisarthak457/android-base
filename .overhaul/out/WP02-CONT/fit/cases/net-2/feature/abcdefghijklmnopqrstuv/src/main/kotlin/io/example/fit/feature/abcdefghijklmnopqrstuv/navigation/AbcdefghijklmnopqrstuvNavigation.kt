package io.example.fit.feature.abcdefghijklmnopqrstuv.navigation

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
import io.example.fit.feature.abcdefghijklmnopqrstuv.api.AbcdefghijklmnopqrstuvDetailKey
import io.example.fit.feature.abcdefghijklmnopqrstuv.api.AbcdefghijklmnopqrstuvListKey
import io.example.fit.feature.abcdefghijklmnopqrstuv.detail.AbcdefghijklmnopqrstuvDetailRoute
import io.example.fit.feature.abcdefghijklmnopqrstuv.list.AbcdefghijklmnopqrstuvListRoute
import kotlinx.serialization.modules.SerializersModule

/**
 * How this feature joins the navigation graph: what renders each key, and how the keys are saved.
 * On a wide window the detail opens beside the list, because the two declare their panes.
 */
@Module
@InstallIn(ActivityRetainedComponent::class)
internal object AbcdefghijklmnopqrstuvNavigation {

    @Provides
    @IntoSet
    fun abcdefghijklmnopqrstuvNavGraph(navigator: AppNavigator): NavGraphEntry = navGraph {
        entry<AbcdefghijklmnopqrstuvListKey>(pane = NavPane.List) {
            AbcdefghijklmnopqrstuvListRoute(navigator = navigator)
        }
        entry<AbcdefghijklmnopqrstuvDetailKey>(pane = NavPane.Detail) { key ->
            AbcdefghijklmnopqrstuvDetailRoute(id = key.id, navigator = navigator)
        }
    }

    @Provides
    @IntoSet
    fun abcdefghijklmnopqrstuvNavKeys(): SerializersModule = navKeys {
        subclass(AbcdefghijklmnopqrstuvListKey::class, AbcdefghijklmnopqrstuvListKey.serializer())
        subclass(
            AbcdefghijklmnopqrstuvDetailKey::class,
            AbcdefghijklmnopqrstuvDetailKey.serializer(),
        )
    }
}
