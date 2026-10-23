package io.example.fitcnet.feature.abc.detail

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.example.fitcnet.core.common.LoadState
import io.example.fitcnet.core.common.result.AppResult
import io.example.fitcnet.core.mvi.MviViewModel
import io.example.fitcnet.data.abc.api.AbcRepository

/** The id arrives through assisted injection, at construction, from the navigation key. */
@HiltViewModel(assistedFactory = AbcDetailViewModel.Factory::class)
internal class AbcDetailViewModel @AssistedInject constructor(
    private val repository: AbcRepository,
    @Assisted private val id: String,
) : MviViewModel<AbcDetailState, AbcDetailEvent, AbcDetailEffect>(
    initial = AbcDetailState(),
) {

    @AssistedFactory
    interface Factory {
        fun create(id: String): AbcDetailViewModel
    }

    override fun handleEvent(event: AbcDetailEvent) {
        when (event) {
            AbcDetailEvent.Appeared -> {
                if (current.loadState == LoadState.Idle) load()
            }

            AbcDetailEvent.Retry -> load()

            AbcDetailEvent.BackClicked -> {
                emit(AbcDetailEffect.NavigateBack)
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
