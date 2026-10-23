package io.example.fit.feature.abcdefghijkl.detail

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.example.fit.core.common.LoadState
import io.example.fit.core.common.result.AppResult
import io.example.fit.core.mvi.MviViewModel
import io.example.fit.data.abcdefghijkl.api.AbcdefghijklRepository

/** The id arrives through assisted injection, at construction, from the navigation key. */
@HiltViewModel(assistedFactory = AbcdefghijklDetailViewModel.Factory::class)
internal class AbcdefghijklDetailViewModel @AssistedInject constructor(
    private val repository: AbcdefghijklRepository,
    @Assisted private val id: String,
) : MviViewModel<AbcdefghijklDetailState, AbcdefghijklDetailEvent, AbcdefghijklDetailEffect>(
    initial = AbcdefghijklDetailState(),
) {

    @AssistedFactory
    interface Factory {
        fun create(id: String): AbcdefghijklDetailViewModel
    }

    override fun handleEvent(event: AbcdefghijklDetailEvent) {
        when (event) {
            AbcdefghijklDetailEvent.Appeared -> {
                if (current.loadState == LoadState.Idle) load()
            }

            AbcdefghijklDetailEvent.Retry -> load()

            AbcdefghijklDetailEvent.BackClicked -> {
                emit(AbcdefghijklDetailEffect.NavigateBack)
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
