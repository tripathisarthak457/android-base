package io.example.audit.data.order.remote

import io.example.audit.core.common.result.AppResult
import io.example.audit.core.network.client.NetworkClient
import io.example.audit.core.network.client.get
import io.example.audit.data.order.remote.dto.OrderDto
import javax.inject.Inject

/** The order endpoints of the app's backend. */
internal class OrderApi @Inject constructor(private val client: NetworkClient) {
    suspend fun items(): AppResult<List<OrderDto>> = client.get(PATH)

    suspend fun item(id: String): AppResult<OrderDto> = client.get("$PATH/$id")

    private companion object {
        /** Relative to the backend's base URL, so every call carries the session's token. */
        const val PATH = "order"
    }
}
