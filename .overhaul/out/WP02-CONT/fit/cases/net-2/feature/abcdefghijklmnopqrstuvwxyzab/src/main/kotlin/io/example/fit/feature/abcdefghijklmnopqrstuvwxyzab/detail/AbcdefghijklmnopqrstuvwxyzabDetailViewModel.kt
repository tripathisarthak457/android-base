package io.example.fit.feature.abcdefghijklmnopqrstuvwxyzab.detail

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.example.fit.core.common.LoadState
import io.example.fit.core.common.result.AppResult
import io.example.fit.core.mvi.MviViewModel
import io.example.fit.data.abcdefghijklmnopqrstuvwxyzab.api.AbcdefghijklmnopqrstuvwxyzabRepository

/** The id arrives through assisted injection, at construction, from the navigation key. */
@HiltViewModel(assistedFactory = AbcdefghijklmnopqrstuvwxyzabDetailViewModel.Factory::class)
internal class AbcdefghijklmnopqrstuvwxyzabDetailViewModel @AssistedInject constructor(
    private val repository: AbcdefghijklmnopqrstuvwxyzabRepository,
    @Assisted private val id: String,
) : MviViewModel<
    AbcdefghijklmnopqrstuvwxyzabDetailState,
    AbcdefghijklmnopqrstuvwxyzabDetailEvent,
    AbcdefghijklmnopqrstuvwxyzabDetailEffect,
    >(
    initial = AbcdefghijklmnopqrstuvwxyzabDetailState(),
) {

    @AssistedFactory
    interface Factory {
        fun create(id: String): AbcdefghijklmnopqrstuvwxyzabDetailViewModel
    }

    override fun handleEvent(event: AbcdefghijklmnopqrstuvwxyzabDetailEvent) {
        when (event) {
            AbcdefghijklmnopqrstuvwxyzabDetailEvent.Appeared -> {
                if (current.loadState == LoadState.Idle) load()
            }

            AbcdefghijklmnopqrstuvwxyzabDetailEvent.Retry -> load()

            AbcdefghijklmnopqrstuvwxyzabDetailEvent.BackClicked -> {
                emit(AbcdefghijklmnopqrstuvwxyzabDetailEffect.NavigateBack)
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
