package com.example.modulescompact.data.orders.api

import com.example.modulescompact.core.common.result.AppResult

/**
 * Answers with [result] and counts the list loads, so a test can check that a screen does not load
 * twice. Set [result] between events to script a failure or a recovery.
 */
class FakeOrdersRepository(var result: AppResult<List<OrdersItem>>) : OrdersRepository {

    var loads = 0
        private set

    override suspend fun items(forceRefresh: Boolean): AppResult<List<OrdersItem>> {
        loads++
        return result
    }

    override suspend fun item(id: String): AppResult<OrdersItem> = when (val current = result) {
        is AppResult.Success -> AppResult.Success(current.value.first { it.id == id })
        is AppResult.Failure -> current
    }
}
