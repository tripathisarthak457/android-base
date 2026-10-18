package com.example.wp04.base.feature.orders

import com.example.wp04.base.core.navigation.AppNavKey
import kotlinx.serialization.Serializable

/** This feature's destinations. Keys carry ids, never models: they are saved in the bundle. */
@Serializable
data object OrdersListKey : AppNavKey

@Serializable
data class OrdersDetailKey(val itemId: Int) : AppNavKey
