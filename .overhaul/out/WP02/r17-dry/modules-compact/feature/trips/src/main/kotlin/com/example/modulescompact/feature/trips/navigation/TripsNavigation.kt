package com.example.modulescompact.feature.trips.navigation

import com.example.modulescompact.core.navigation.AppNavigator
import com.example.modulescompact.core.navigation.NavGraphEntry
import com.example.modulescompact.core.navigation.NavPane
import com.example.modulescompact.core.navigation.navGraph
import com.example.modulescompact.core.navigation.navKeys
import com.example.modulescompact.feature.trips.api.TripsDetailKey
import com.example.modulescompact.feature.trips.api.TripsListKey
import com.example.modulescompact.feature.trips.detail.TripsDetailRoute
import com.example.modulescompact.feature.trips.list.TripsListRoute
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.serialization.modules.SerializersModule

/**
 * How this feature joins the navigation graph: what renders each key, and how the keys are saved.
 * On a wide window the detail opens beside the list, because the two declare their panes.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object TripsNavigation {

    @Provides
    @IntoSet
    fun tripsNavGraph(navigator: AppNavigator): NavGraphEntry = navGraph {
        entry<TripsListKey>(pane = NavPane.List) {
            TripsListRoute(navigator = navigator)
        }
        entry<TripsDetailKey>(pane = NavPane.Detail) { key ->
            TripsDetailRoute(id = key.id, navigator = navigator)
        }
    }

    @Provides
    @IntoSet
    fun tripsNavKeys(): SerializersModule = navKeys {
        subclass(TripsListKey::class, TripsListKey.serializer())
        subclass(TripsDetailKey::class, TripsDetailKey.serializer())
    }
}
