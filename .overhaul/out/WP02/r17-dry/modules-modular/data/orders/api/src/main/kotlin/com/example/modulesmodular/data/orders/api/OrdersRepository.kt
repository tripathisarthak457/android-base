package com.example.modulesmodular.data.orders.api

import com.example.modulesmodular.core.common.result.AppResult

/**
 * Everything the app can ask about orders.
 * An interface, so ViewModel tests use a fake.
 */
interface OrdersRepository {

    suspend fun items(forceRefresh: Boolean = false): AppResult<List<OrdersItem>>

    suspend fun item(id: String): AppResult<OrdersItem>
}
