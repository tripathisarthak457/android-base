package io.example.fit.feature.abcdefghijklm.detail

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.example.fit.core.common.LoadState
import io.example.fit.core.common.result.AppResult
import io.example.fit.core.mvi.MviViewModel
import io.example.fit.data.abcdefghijklm.api.AbcdefghijklmRepository

/** The id arrives through assisted injection, at construction, from the navigation key. */
@HiltViewModel(assistedFactory = AbcdefghijklmDetailViewModel.Factory::class)
internal class AbcdefghijklmDetailViewModel @AssistedInject constructor(
    private val repository: AbcdefghijklmRepository,
    @Assisted private val id: String,
) : MviViewModel<AbcdefghijklmDetailState, AbcdefghijklmDetailEvent, AbcdefghijklmDetailEffect>(
    initial = AbcdefghijklmDetailState(),
) {

    @AssistedFactory
    interface Factory {
        fun create(id: String): AbcdefghijklmDetailViewModel
    }

    override fun handleEvent(event: AbcdefghijklmDetailEvent) {
        when (event) {
            AbcdefghijklmDetailEvent.Appeared -> {
                if (current.loadState == LoadState.Idle) load()
            }

            AbcdefghijklmDetailEvent.Retry -> load()

            AbcdefghijklmDetailEvent.BackClicked -> {
                emit(AbcdefghijklmDetailEffect.NavigateBack)
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
