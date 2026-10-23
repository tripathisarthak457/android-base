package io.example.fit.feature.abcdefghijklmnopqrstuvwx.detail

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.example.fit.core.common.LoadState
import io.example.fit.core.common.result.AppResult
import io.example.fit.core.mvi.MviViewModel
import io.example.fit.data.abcdefghijklmnopqrstuvwx.api.AbcdefghijklmnopqrstuvwxRepository

/** The id arrives through assisted injection, at construction, from the navigation key. */
@HiltViewModel(assistedFactory = AbcdefghijklmnopqrstuvwxDetailViewModel.Factory::class)
internal class AbcdefghijklmnopqrstuvwxDetailViewModel @AssistedInject constructor(
    private val repository: AbcdefghijklmnopqrstuvwxRepository,
    @Assisted private val id: String,
) : MviViewModel<
    AbcdefghijklmnopqrstuvwxDetailState,
    AbcdefghijklmnopqrstuvwxDetailEvent,
    AbcdefghijklmnopqrstuvwxDetailEffect,
    >(
    initial = AbcdefghijklmnopqrstuvwxDetailState(),
) {

    @AssistedFactory
    interface Factory {
        fun create(id: String): AbcdefghijklmnopqrstuvwxDetailViewModel
    }

    override fun handleEvent(event: AbcdefghijklmnopqrstuvwxDetailEvent) {
        when (event) {
            AbcdefghijklmnopqrstuvwxDetailEvent.Appeared -> {
                if (current.loadState == LoadState.Idle) load()
            }

            AbcdefghijklmnopqrstuvwxDetailEvent.Retry -> load()

            AbcdefghijklmnopqrstuvwxDetailEvent.BackClicked -> {
                emit(AbcdefghijklmnopqrstuvwxDetailEffect.NavigateBack)
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
