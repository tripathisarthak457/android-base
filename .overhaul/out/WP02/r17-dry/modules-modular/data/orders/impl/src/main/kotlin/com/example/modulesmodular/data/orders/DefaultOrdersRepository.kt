package com.example.modulesmodular.data.orders

import com.example.modulesmodular.core.common.result.AppResult
import com.example.modulesmodular.core.common.result.map
import com.example.modulesmodular.core.network.NetworkClient
import com.example.modulesmodular.core.network.get
import com.example.modulesmodular.core.network.model.CachePolicy
import com.example.modulesmodular.data.orders.api.OrdersItem
import com.example.modulesmodular.data.orders.api.OrdersRepository
import com.example.modulesmodular.data.orders.remote.OrdersDto
import com.example.modulesmodular.data.orders.remote.toDomain
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultOrdersRepository @Inject constructor(
    private val networkClient: NetworkClient,
) : OrdersRepository {

    /** Cached, so reopening the screen or going offline shows the last answer. */
    override suspend fun items(forceRefresh: Boolean): AppResult<List<OrdersItem>> =
        networkClient.get<List<OrdersDto>>(
            path = PATH,
            cache = CachePolicy.Enabled(
                key = PATH,
                maxAgeMillis = CACHE_MAX_AGE_MILLIS,
                forceRefresh = forceRefresh,
            ),
        ).map { dtos -> dtos.map(OrdersDto::toDomain) }

    override suspend fun item(id: String): AppResult<OrdersItem> = networkClient.get<OrdersDto>(
        path = "$PATH/$id",
        cache = CachePolicy.Enabled(key = "$PATH/$id", maxAgeMillis = CACHE_MAX_AGE_MILLIS),
    ).map(OrdersDto::toDomain)

    private companion object {
        const val PATH = "orders"
        const val CACHE_MAX_AGE_MILLIS = 5 * 60 * 1000L
    }
}
