package com.example.modulescompact.data.orders.remote

import com.example.modulescompact.data.orders.api.OrdersItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The wire format, exactly as the backend sends it. */
@Serializable
internal data class OrdersDto(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String = "",
)

internal fun OrdersDto.toDomain(): OrdersItem = OrdersItem(id = id, title = title.trim())
