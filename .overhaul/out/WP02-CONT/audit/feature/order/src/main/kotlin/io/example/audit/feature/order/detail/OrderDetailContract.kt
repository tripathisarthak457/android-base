package io.example.audit.feature.order.detail

import androidx.compose.runtime.Immutable
import io.example.audit.core.common.LoadState
import io.example.audit.core.mvi.UiEffect
import io.example.audit.core.mvi.UiEvent
import io.example.audit.core.mvi.UiState
import io.example.audit.data.order.api.model.OrderItem

@Immutable
internal data class OrderDetailState(
    val loadState: LoadState = LoadState.Idle,
    val item: OrderItem? = null,
) : UiState

internal sealed interface OrderDetailEvent : UiEvent {
    data object Appeared : OrderDetailEvent
    data object Retry : OrderDetailEvent
    data object BackClicked : OrderDetailEvent
}

internal sealed interface OrderDetailEffect : UiEffect {
    data object NavigateBack : OrderDetailEffect
}
