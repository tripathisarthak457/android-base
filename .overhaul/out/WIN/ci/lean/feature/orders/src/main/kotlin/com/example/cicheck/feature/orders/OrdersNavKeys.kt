package com.example.cicheck.feature.orders

import com.example.cicheck.core.common.navigation.AppNavKey
import kotlinx.serialization.Serializable

/** This feature's destinations. Keys carry ids, never models: they are saved in the bundle. */
@Serializable
data object OrdersListKey : AppNavKey

@Serializable
data class OrdersDetailKey(val itemId: Int) : AppNavKey
