package com.example.wp04.base.feature.orders.di

import com.example.wp04.base.core.navigation.AppNavigator
import com.example.wp04.base.core.navigation.NavGraphEntry
import com.example.wp04.base.core.navigation.NavPane
import com.example.wp04.base.core.navigation.navGraph
import com.example.wp04.base.core.navigation.navKeys
import com.example.wp04.base.feature.orders.OrdersDetailKey
import com.example.wp04.base.feature.orders.OrdersListKey
import com.example.wp04.base.feature.orders.OrdersListRoute
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.serialization.modules.SerializersModule

/**
 * Registers this feature's screens and key serializers. Without the serializers the back stack
 * does not survive process death.
 *
 * `OrdersDetailKey` has no screen yet: add `entry<OrdersDetailKey>(pane = NavPane.Detail) { … }`
 * with it, and on a wide window it opens beside the list.
 */
@Module
@InstallIn(SingletonComponent::class)
object OrdersNavModule {
    @Provides
    @IntoSet
    fun ordersNavGraph(navigator: AppNavigator): NavGraphEntry = navGraph {
        entry<OrdersListKey>(pane = NavPane.List) { OrdersListRoute(navigator = navigator) }
    }

    @Provides
    @IntoSet
    fun ordersNavKeys(): SerializersModule = navKeys {
        subclass(OrdersListKey::class, OrdersListKey.serializer())
        subclass(OrdersDetailKey::class, OrdersDetailKey.serializer())
    }
}
