package com.example.wp04.base.feature.orders

import androidx.compose.runtime.Immutable
import com.example.wp04.base.core.common.mvi.LoadState
import com.example.wp04.base.core.common.mvi.UiEffect
import com.example.wp04.base.core.common.mvi.UiEvent
import com.example.wp04.base.core.common.mvi.UiState
import com.example.wp04.base.data.orders.OrdersItem

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
