package com.example.modulesmodular.feature.orders.detail

import com.example.modulesmodular.core.common.LoadState
import com.example.modulesmodular.core.common.result.AppResult
import com.example.modulesmodular.core.mvi.MviViewModel
import com.example.modulesmodular.data.orders.api.OrdersRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

/** The id arrives through assisted injection, at construction, from the navigation key. */
@HiltViewModel(assistedFactory = OrdersDetailViewModel.Factory::class)
internal class OrdersDetailViewModel @AssistedInject constructor(
    private val repository: OrdersRepository,
    @Assisted private val id: String,
) : MviViewModel<OrdersDetailState, OrdersDetailEvent, OrdersDetailEffect>(
    initial = OrdersDetailState(),
) {

    @AssistedFactory
    interface Factory {
        fun create(id: String): OrdersDetailViewModel
    }

    override fun handleEvent(event: OrdersDetailEvent) {
        when (event) {
            OrdersDetailEvent.Appeared -> {
                if (current.loadState == LoadState.Idle) load()
            }

            OrdersDetailEvent.Retry -> load()

            OrdersDetailEvent.BackClicked -> {
                emit(OrdersDetailEffect.NavigateBack)
            }
        }
    }

    private fun load() {
        launchLatest(LOAD) {
            setState { copy(loadState = LoadState.Loading) }
            when (val result = repository.item(id)) {
                is AppResult.Success -> setState {
                    copy(loadState = LoadState.Success, item = result.value)
                }

                is AppResult.Failure -> setState {
                    copy(loadState = LoadState.Error(result.error))
                }
            }
        }
    }

    private companion object {
        const val LOAD = "load"
    }
}
