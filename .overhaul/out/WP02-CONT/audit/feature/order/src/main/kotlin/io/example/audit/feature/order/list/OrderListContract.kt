package io.example.audit.feature.order.list

import androidx.compose.runtime.Immutable
import io.example.audit.core.common.LoadState
import io.example.audit.core.mvi.UiEffect
import io.example.audit.core.mvi.UiEvent
import io.example.audit.core.mvi.UiState
import io.example.audit.data.order.api.model.OrderItem

@Immutable
internal data class OrderListState(
    val loadState: LoadState = LoadState.Idle,
    val items: List<OrderItem> = emptyList(),
) : UiState

internal sealed interface OrderListEvent : UiEvent {
    data object Appeared : OrderListEvent
    data object Refresh : OrderListEvent
    data object Retry : OrderListEvent
    data class ItemClicked(val id: String) : OrderListEvent
}

internal sealed interface OrderListEffect : UiEffect {
    data class OpenDetail(val id: String) : OrderListEffect
}
