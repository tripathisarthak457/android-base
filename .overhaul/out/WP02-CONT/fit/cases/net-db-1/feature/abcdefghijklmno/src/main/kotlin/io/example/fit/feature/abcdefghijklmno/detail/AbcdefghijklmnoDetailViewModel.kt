package io.example.fit.feature.abcdefghijklmno.detail

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.example.fit.core.common.LoadState
import io.example.fit.core.common.result.AppResult
import io.example.fit.core.mvi.MviViewModel
import io.example.fit.data.abcdefghijklmno.api.AbcdefghijklmnoRepository

/** The id arrives through assisted injection, at construction, from the navigation key. */
@HiltViewModel(assistedFactory = AbcdefghijklmnoDetailViewModel.Factory::class)
internal class AbcdefghijklmnoDetailViewModel @AssistedInject constructor(
    private val repository: AbcdefghijklmnoRepository,
    @Assisted private val id: String,
) : MviViewModel<
    AbcdefghijklmnoDetailState,
    AbcdefghijklmnoDetailEvent,
    AbcdefghijklmnoDetailEffect,
    >(
    initial = AbcdefghijklmnoDetailState(),
) {

    @AssistedFactory
    interface Factory {
        fun create(id: String): AbcdefghijklmnoDetailViewModel
    }

    override fun handleEvent(event: AbcdefghijklmnoDetailEvent) {
        when (event) {
            AbcdefghijklmnoDetailEvent.Appeared -> {
                if (current.loadState == LoadState.Idle) load()
            }

            AbcdefghijklmnoDetailEvent.Retry -> load()

            AbcdefghijklmnoDetailEvent.BackClicked -> {
                emit(AbcdefghijklmnoDetailEffect.NavigateBack)
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
