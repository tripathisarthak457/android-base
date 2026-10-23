package io.example.fit.feature.abcdefghijk.detail

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.example.fit.core.common.LoadState
import io.example.fit.core.common.result.AppResult
import io.example.fit.core.mvi.MviViewModel
import io.example.fit.data.abcdefghijk.api.AbcdefghijkRepository

/** The id arrives through assisted injection, at construction, from the navigation key. */
@HiltViewModel(assistedFactory = AbcdefghijkDetailViewModel.Factory::class)
internal class AbcdefghijkDetailViewModel @AssistedInject constructor(
    private val repository: AbcdefghijkRepository,
    @Assisted private val id: String,
) : MviViewModel<AbcdefghijkDetailState, AbcdefghijkDetailEvent, AbcdefghijkDetailEffect>(
    initial = AbcdefghijkDetailState(),
) {

    @AssistedFactory
    interface Factory {
        fun create(id: String): AbcdefghijkDetailViewModel
    }

    override fun handleEvent(event: AbcdefghijkDetailEvent) {
        when (event) {
            AbcdefghijkDetailEvent.Appeared -> {
                if (current.loadState == LoadState.Idle) load()
            }

            AbcdefghijkDetailEvent.Retry -> load()

            AbcdefghijkDetailEvent.BackClicked -> {
                emit(AbcdefghijkDetailEffect.NavigateBack)
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
