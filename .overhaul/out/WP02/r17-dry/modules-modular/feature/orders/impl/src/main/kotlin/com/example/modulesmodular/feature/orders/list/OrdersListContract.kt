package com.example.modulesmodular.feature.orders.list

import androidx.compose.runtime.Immutable
import com.example.modulesmodular.core.common.LoadState
import com.example.modulesmodular.core.mvi.UiEffect
import com.example.modulesmodular.core.mvi.UiEvent
import com.example.modulesmodular.core.mvi.UiState
import com.example.modulesmodular.data.orders.api.OrdersItem

@Immutable
internal data class OrdersListState(
    val loadState: LoadState = LoadState.Idle,
    val items: List<OrdersItem> = emptyList(),
) : UiState

internal sealed interface OrdersListEvent : UiEvent {
    data object Appeared : OrdersListEvent
    data object Refresh : OrdersListEvent
    data object Retry : OrdersListEvent
    data class ItemClicked(val id: String) : OrdersListEvent
}

internal sealed interface OrdersListEffect : UiEffect {
    data class OpenDetail(val id: String) : OrdersListEffect
}
