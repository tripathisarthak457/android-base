package io.example.audit.data.order.local

import io.example.audit.core.database.order.OrderEntity
import io.example.audit.data.order.api.model.OrderItem
import io.example.audit.data.order.remote.dto.OrderDto
import io.example.audit.data.order.remote.toDomain

/** [position] is the row's place in the server's list, which the table keeps for ordering. */
internal fun OrderDto.toEntity(position: Int): OrderEntity = toDomain().let { item ->
    OrderEntity(id = item.id, title = item.title, position = position)
}

internal fun OrderEntity.toDomain(): OrderItem = OrderItem(id = id, title = title)
