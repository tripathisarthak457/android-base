package com.example.modulesmodular.feature.orders.detail

import androidx.compose.runtime.Immutable
import com.example.modulesmodular.core.common.LoadState
import com.example.modulesmodular.core.mvi.UiEffect
import com.example.modulesmodular.core.mvi.UiEvent
import com.example.modulesmodular.core.mvi.UiState
import com.example.modulesmodular.data.orders.api.OrdersItem

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
