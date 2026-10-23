package io.example.fit.feature.abcde.detail

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.example.fit.core.common.LoadState
import io.example.fit.core.common.result.AppResult
import io.example.fit.core.mvi.MviViewModel
import io.example.fit.data.abcde.api.AbcdeRepository

/** The id arrives through assisted injection, at construction, from the navigation key. */
@HiltViewModel(assistedFactory = AbcdeDetailViewModel.Factory::class)
internal class AbcdeDetailViewModel @AssistedInject constructor(
    private val repository: AbcdeRepository,
    @Assisted private val id: String,
) : MviViewModel<AbcdeDetailState, AbcdeDetailEvent, AbcdeDetailEffect>(
    initial = AbcdeDetailState(),
) {

    @AssistedFactory
    interface Factory {
        fun create(id: String): AbcdeDetailViewModel
    }

    override fun handleEvent(event: AbcdeDetailEvent) {
        when (event) {
            AbcdeDetailEvent.Appeared -> {
                if (current.loadState == LoadState.Idle) load()
            }

            AbcdeDetailEvent.Retry -> load()

            AbcdeDetailEvent.BackClicked -> {
                emit(AbcdeDetailEffect.NavigateBack)
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
