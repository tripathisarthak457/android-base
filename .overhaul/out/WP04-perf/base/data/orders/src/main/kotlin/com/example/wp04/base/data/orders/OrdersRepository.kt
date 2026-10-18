package com.example.wp04.base.data.orders

import com.example.wp04.base.core.common.AppResult
import com.example.wp04.base.core.network.NetworkClient
import com.example.wp04.base.core.network.get
import javax.inject.Inject
import javax.inject.Singleton

/** The domain model for Orders. Add a DTO and a mapper once the wire format differs. */
data class OrdersItem(
    val id: Int,
    val title: String,
)

/** Everything the app can ask about Orders. An interface, so tests can use a fake. */
interface OrdersRepository {
    suspend fun items(): AppResult<List<OrdersItem>>
}

@Singleton
class DefaultOrdersRepository @Inject constructor(
    private val networkClient: NetworkClient,
) : OrdersRepository {
    override suspend fun items(): AppResult<List<OrdersItem>> =
        networkClient.get<List<OrdersItem>>(path = ITEMS_PATH)

    private companion object {
        const val ITEMS_PATH = "orders"
    }
}
