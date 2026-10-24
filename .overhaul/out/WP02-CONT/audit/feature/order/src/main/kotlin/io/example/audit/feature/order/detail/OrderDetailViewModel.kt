package io.example.audit.feature.order.detail

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.example.audit.core.common.LoadState
import io.example.audit.core.common.result.AppResult
import io.example.audit.core.mvi.MviViewModel
import io.example.audit.data.order.api.OrderRepository

/** The id arrives through assisted injection, at construction, from the navigation key. */
@HiltViewModel(assistedFactory = OrderDetailViewModel.Factory::class)
internal class OrderDetailViewModel @AssistedInject constructor(
    private val repository: OrderRepository,
    @Assisted private val id: String,
) : MviViewModel<OrderDetailState, OrderDetailEvent, OrderDetailEffect>(
    initial = OrderDetailState(),
) {

    @AssistedFactory
    interface Factory {
        fun create(id: String): OrderDetailViewModel
    }

    override fun handleEvent(event: OrderDetailEvent) {
        when (event) {
            OrderDetailEvent.Appeared -> {
                if (current.loadState == LoadState.Idle) load()
            }

            OrderDetailEvent.Retry -> load()

            OrderDetailEvent.BackClicked -> {
                emit(OrderDetailEffect.NavigateBack)
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
