package io.example.audit.feature.order.navigation

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet
import io.example.audit.core.navigation.AppNavigator
import io.example.audit.core.navigation.NavGraphEntry
import io.example.audit.core.navigation.NavPane
import io.example.audit.core.navigation.navGraph
import io.example.audit.core.navigation.navKeys
import io.example.audit.feature.order.api.OrderDetailKey
import io.example.audit.feature.order.api.OrderListKey
import io.example.audit.feature.order.detail.OrderDetailRoute
import io.example.audit.feature.order.list.OrderListRoute
import kotlinx.serialization.modules.SerializersModule

/**
 * How this feature joins the navigation graph: what renders each key, and how the keys are saved.
 * On a wide window the detail opens beside the list, because the two declare their panes.
 */
@Module
@InstallIn(ActivityRetainedComponent::class)
internal object OrderNavigation {

    @Provides
    @IntoSet
    fun orderNavGraph(navigator: AppNavigator): NavGraphEntry = navGraph {
        entry<OrderListKey>(pane = NavPane.List) {
            OrderListRoute(navigator = navigator)
        }
        entry<OrderDetailKey>(pane = NavPane.Detail) { key ->
            OrderDetailRoute(id = key.id, navigator = navigator)
        }
    }

    @Provides
    @IntoSet
    fun orderNavKeys(): SerializersModule = navKeys {
        subclass(OrderListKey::class, OrderListKey.serializer())
        subclass(OrderDetailKey::class, OrderDetailKey.serializer())
    }
}
