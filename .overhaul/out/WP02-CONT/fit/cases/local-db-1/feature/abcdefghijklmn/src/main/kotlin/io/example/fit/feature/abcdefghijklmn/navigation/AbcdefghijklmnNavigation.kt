package io.example.fit.feature.abcdefghijklmn.navigation

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
import io.example.fit.feature.abcdefghijklmn.api.AbcdefghijklmnDetailKey
import io.example.fit.feature.abcdefghijklmn.api.AbcdefghijklmnListKey
import io.example.fit.feature.abcdefghijklmn.detail.AbcdefghijklmnDetailRoute
import io.example.fit.feature.abcdefghijklmn.list.AbcdefghijklmnListRoute
import kotlinx.serialization.modules.SerializersModule

/**
 * How this feature joins the navigation graph: what renders each key, and how the keys are saved.
 * On a wide window the detail opens beside the list, because the two declare their panes.
 */
@Module
@InstallIn(ActivityRetainedComponent::class)
internal object AbcdefghijklmnNavigation {

    @Provides
    @IntoSet
    fun abcdefghijklmnNavGraph(navigator: AppNavigator): NavGraphEntry = navGraph {
        entry<AbcdefghijklmnListKey>(pane = NavPane.List) {
            AbcdefghijklmnListRoute(navigator = navigator)
        }
        entry<AbcdefghijklmnDetailKey>(pane = NavPane.Detail) { key ->
            AbcdefghijklmnDetailRoute(id = key.id, navigator = navigator)
        }
    }

    @Provides
    @IntoSet
    fun abcdefghijklmnNavKeys(): SerializersModule = navKeys {
        subclass(AbcdefghijklmnListKey::class, AbcdefghijklmnListKey.serializer())
        subclass(AbcdefghijklmnDetailKey::class, AbcdefghijklmnDetailKey.serializer())
    }
}
