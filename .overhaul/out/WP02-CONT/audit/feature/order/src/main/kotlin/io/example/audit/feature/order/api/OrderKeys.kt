package io.example.audit.feature.order.api

import io.example.audit.core.common.navigation.AppNavKey
import kotlinx.serialization.Serializable

/**
 * This feature's destinations and its whole public surface. Keys carry ids, never models: they are
 * saved with the back stack and restored after the process dies.
 */
@Serializable
data object OrderListKey : AppNavKey

@Serializable
data class OrderDetailKey(val id: String) : AppNavKey
