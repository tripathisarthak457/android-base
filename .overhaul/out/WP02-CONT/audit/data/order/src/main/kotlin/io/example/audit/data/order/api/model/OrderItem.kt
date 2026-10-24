package io.example.audit.data.order.api.model

/** One order entry as the app sees it, whatever shape the source sends. */
data class OrderItem(val id: String, val title: String)
