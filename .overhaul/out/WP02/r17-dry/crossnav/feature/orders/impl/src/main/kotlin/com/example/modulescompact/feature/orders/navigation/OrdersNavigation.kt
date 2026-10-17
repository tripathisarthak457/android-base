package com.example.modulescompact.feature.orders.navigation

import com.example.modulescompact.core.navigation.AppNavigator
import com.example.modulescompact.core.navigation.NavGraphEntry
import com.example.modulescompact.core.navigation.NavPane
import com.example.modulescompact.core.navigation.navGraph
import com.example.modulescompact.core.navigation.navKeys
import com.example.modulescompact.feature.orders.api.OrderDetailKey
import com.example.modulescompact.feature.orders.api.OrdersDetailKey
import com.example.modulescompact.feature.orders.api.OrdersListKey
import com.example.modulescompact.feature.orders.detail.OrdersDetailRoute
import com.example.modulescompact.feature.orders.list.OrdersListRoute
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
internal object OrdersNavigation {

    @Provides
    @IntoSet
    fun ordersNavGraph(navigator: AppNavigator): NavGraphEntry = navGraph {
        entry<OrdersListKey>(pane = NavPane.List) {
            OrdersListRoute(navigator = navigator)
        }
        entry<OrdersDetailKey>(pane = NavPane.Detail) { key ->
            OrdersDetailRoute(id = key.id, navigator = navigator)
        }
        entry<OrderDetailKey>(pane = NavPane.Detail) { key ->
            OrdersDetailRoute(id = key.id, navigator = navigator)
        }
    }

    @Provides
    @IntoSet
    fun ordersNavKeys(): SerializersModule = navKeys {
        subclass(OrdersListKey::class, OrdersListKey.serializer())
        subclass(OrdersDetailKey::class, OrdersDetailKey.serializer())
        subclass(OrderDetailKey::class, OrderDetailKey.serializer())
    }
}
