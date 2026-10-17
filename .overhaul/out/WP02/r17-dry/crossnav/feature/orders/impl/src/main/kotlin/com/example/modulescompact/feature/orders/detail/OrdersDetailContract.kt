package com.example.modulescompact.feature.orders.detail

import androidx.compose.runtime.Immutable
import com.example.modulescompact.core.common.LoadState
import com.example.modulescompact.core.mvi.UiEffect
import com.example.modulescompact.core.mvi.UiEvent
import com.example.modulescompact.core.mvi.UiState
import com.example.modulescompact.data.orders.api.OrdersItem

@Immutable
internal data class OrdersDetailState(
    val loadState: LoadState = LoadState.Idle,
    val item: OrdersItem? = null,
) : UiState

internal sealed interface OrdersDetailEvent : UiEvent {
    data object Appeared : OrdersDetailEvent
    data object Retry : OrdersDetailEvent
    data object BackClicked : OrdersDetailEvent
}

internal sealed interface OrdersDetailEffect : UiEffect {
    data object NavigateBack : OrdersDetailEffect
}
