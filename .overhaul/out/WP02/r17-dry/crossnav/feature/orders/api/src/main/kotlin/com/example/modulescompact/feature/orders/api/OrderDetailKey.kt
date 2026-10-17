package com.example.modulescompact.feature.orders.api

import com.example.modulescompact.core.common.navigation.AppNavKey
import kotlinx.serialization.Serializable

/** One order, opened from any feature that shows an order id. */
@Serializable
data class OrderDetailKey(val id: String) : AppNavKey
