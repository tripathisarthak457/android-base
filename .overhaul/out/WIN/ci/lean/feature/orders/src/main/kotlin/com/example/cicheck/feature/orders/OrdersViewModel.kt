package com.example.cicheck.feature.orders

import com.example.cicheck.core.common.LoadState
import com.example.cicheck.core.common.result.AppResult
import com.example.cicheck.core.mvi.MviViewModel
import com.example.cicheck.data.orders.OrdersRepository
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

    override fun handleEvent(event: OrdersEvent) {
        when (event) {
            OrdersEvent.Load, OrdersEvent.Retry -> load()
            is OrdersEvent.ItemClicked -> emit(OrdersEffect.OpenDetail(event.id))
        }
    }

    private fun load() {
        launchLatest(LOAD) {
            setState { copy(loadState = LoadState.Loading) }

            when (val result = repository.items()) {
                is AppResult.Success -> setState {
                    copy(
                        loadState = if (result.value.isEmpty()) LoadState.Empty else LoadState.Success,
                        items = result.value,
                    )
                }

                is AppResult.Failure -> setState { copy(loadState = LoadState.Error(result.error)) }
            }
        }
    }

    private companion object {
        const val LOAD = "load"
    }
}
