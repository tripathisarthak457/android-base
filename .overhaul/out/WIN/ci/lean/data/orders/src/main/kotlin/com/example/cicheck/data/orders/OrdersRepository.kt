package com.example.cicheck.data.orders

import com.example.cicheck.core.common.result.AppResult
import com.example.cicheck.core.network.NetworkClient
import com.example.cicheck.core.network.get
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/** The domain model for Orders. Add a DTO and a mapper once the wire format differs. */
@Serializable
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
