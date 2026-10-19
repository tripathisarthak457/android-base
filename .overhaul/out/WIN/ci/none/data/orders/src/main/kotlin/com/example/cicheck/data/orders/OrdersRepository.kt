package com.example.cicheck.data.orders

import com.example.cicheck.core.common.result.AppResult
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
class DefaultOrdersRepository @Inject constructor() : OrdersRepository {

    override suspend fun items(): AppResult<List<OrdersItem>> =
        AppResult.Success(emptyList())
}
