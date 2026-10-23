package io.example.fit.feature.abcdefghijklmnopqrstuvwxyzabc.navigation

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
import io.example.fit.feature.abcdefghijklmnopqrstuvwxyzabc.api.AbcdefghijklmnopqrstuvwxyzabcDetailKey
import io.example.fit.feature.abcdefghijklmnopqrstuvwxyzabc.api.AbcdefghijklmnopqrstuvwxyzabcListKey
import io.example.fit.feature.abcdefghijklmnopqrstuvwxyzabc.detail.AbcdefghijklmnopqrstuvwxyzabcDetailRoute
import io.example.fit.feature.abcdefghijklmnopqrstuvwxyzabc.list.AbcdefghijklmnopqrstuvwxyzabcListRoute
import kotlinx.serialization.modules.SerializersModule

/**
 * How this feature joins the navigation graph: what renders each key, and how the keys are saved.
 * On a wide window the detail opens beside the list, because the two declare their panes.
 */
@Module
@InstallIn(ActivityRetainedComponent::class)
internal object AbcdefghijklmnopqrstuvwxyzabcNavigation {

    @Provides
    @IntoSet
    fun abcdefghijklmnopqrstuvwxyzabcNavGraph(navigator: AppNavigator): NavGraphEntry = navGraph {
        entry<AbcdefghijklmnopqrstuvwxyzabcListKey>(pane = NavPane.List) {
            AbcdefghijklmnopqrstuvwxyzabcListRoute(navigator = navigator)
        }
        entry<AbcdefghijklmnopqrstuvwxyzabcDetailKey>(pane = NavPane.Detail) { key ->
            AbcdefghijklmnopqrstuvwxyzabcDetailRoute(id = key.id, navigator = navigator)
        }
    }

    @Provides
    @IntoSet
    fun abcdefghijklmnopqrstuvwxyzabcNavKeys(): SerializersModule = navKeys {
        subclass(
            AbcdefghijklmnopqrstuvwxyzabcListKey::class,
            AbcdefghijklmnopqrstuvwxyzabcListKey.serializer(),
        )
        subclass(
            AbcdefghijklmnopqrstuvwxyzabcDetailKey::class,
            AbcdefghijklmnopqrstuvwxyzabcDetailKey.serializer(),
        )
    }
}
