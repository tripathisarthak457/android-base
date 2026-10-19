package com.example.cicheck.feature.orders

import androidx.compose.runtime.Immutable
import com.example.cicheck.core.common.mvi.LoadState
import com.example.cicheck.core.common.mvi.UiEffect
import com.example.cicheck.core.common.mvi.UiEvent
import com.example.cicheck.core.common.mvi.UiState
import com.example.cicheck.data.orders.OrdersItem

@Immutable
data class OrdersState(
    val loadState: LoadState = LoadState.Idle,
    val items: List<OrdersItem> = emptyList(),
) : UiState

sealed interface OrdersEvent : UiEvent {
    data object Load : OrdersEvent
    data object Retry : OrdersEvent
    data class ItemClicked(val id: Int) : OrdersEvent
}

sealed interface OrdersEffect : UiEffect {
    data class OpenDetail(val id: Int) : OrdersEffect
}
