package io.example.fit.feature.abcdefghijklmnopqrstuvwxyza.detail

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.example.fit.core.common.LoadState
import io.example.fit.core.common.result.AppResult
import io.example.fit.core.mvi.MviViewModel
import io.example.fit.data.abcdefghijklmnopqrstuvwxyza.api.AbcdefghijklmnopqrstuvwxyzaRepository

/** The id arrives through assisted injection, at construction, from the navigation key. */
@HiltViewModel(assistedFactory = AbcdefghijklmnopqrstuvwxyzaDetailViewModel.Factory::class)
internal class AbcdefghijklmnopqrstuvwxyzaDetailViewModel @AssistedInject constructor(
    private val repository: AbcdefghijklmnopqrstuvwxyzaRepository,
    @Assisted private val id: String,
) : MviViewModel<
    AbcdefghijklmnopqrstuvwxyzaDetailState,
    AbcdefghijklmnopqrstuvwxyzaDetailEvent,
    AbcdefghijklmnopqrstuvwxyzaDetailEffect,
    >(
    initial = AbcdefghijklmnopqrstuvwxyzaDetailState(),
) {

    @AssistedFactory
    interface Factory {
        fun create(id: String): AbcdefghijklmnopqrstuvwxyzaDetailViewModel
    }

    override fun handleEvent(event: AbcdefghijklmnopqrstuvwxyzaDetailEvent) {
        when (event) {
            AbcdefghijklmnopqrstuvwxyzaDetailEvent.Appeared -> {
                if (current.loadState == LoadState.Idle) load()
            }

            AbcdefghijklmnopqrstuvwxyzaDetailEvent.Retry -> load()

            AbcdefghijklmnopqrstuvwxyzaDetailEvent.BackClicked -> {
                emit(AbcdefghijklmnopqrstuvwxyzaDetailEffect.NavigateBack)
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
