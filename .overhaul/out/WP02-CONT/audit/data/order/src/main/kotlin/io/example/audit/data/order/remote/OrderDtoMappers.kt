package io.example.audit.data.order.remote

import io.example.audit.data.order.api.model.OrderItem
import io.example.audit.data.order.remote.dto.OrderDto

/**
 * Trims what the wire sends. A blank title stays blank: wording a fallback is the screen's job, in
 * the person's language.
 */
internal fun OrderDto.toDomain(): OrderItem = OrderItem(id = id, title = title.trim())
