package com.example.wp04.base.feature.orders

import com.example.wp04.base.core.common.AppResult
import com.example.wp04.base.core.common.mvi.LoadState
import com.example.wp04.base.core.common.mvi.MviViewModel
import com.example.wp04.base.data.orders.OrdersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Loads in `init`, which runs once per screen, rather than from a `LaunchedEffect`. */
@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val repository: OrdersRepository,
) : MviViewModel<OrdersState, OrdersEvent, OrdersEffect>(OrdersState()) {
    init {
        onEvent(OrdersEvent.Load)
    }

    override suspend fun handleEvent(event: OrdersEvent) {
        when (event) {
            OrdersEvent.Load, OrdersEvent.Retry -> load()
            is OrdersEvent.ItemClicked -> emitEffect(OrdersEffect.OpenDetail(event.id))
        }
    }

    private suspend fun load() {
        updateState { copy(loadState = LoadState.Loading) }

        when (val result = repository.items()) {
            is AppResult.Success -> updateState {
                copy(
                    loadState = if (result.data.isEmpty()) LoadState.Empty else LoadState.Success,
                    items = result.data,
                )
            }

            is AppResult.Failure -> updateState { copy(loadState = result.toLoadState()) }
        }
    }
}
