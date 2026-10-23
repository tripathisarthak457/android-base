package io.example.fit.feature.abcdefghijklmnopqrstuvwxyzabc.detail

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.example.fit.core.common.LoadState
import io.example.fit.core.common.result.AppResult
import io.example.fit.core.mvi.MviViewModel
import io.example.fit.data.abcdefghijklmnopqrstuvwxyzabc.api.AbcdefghijklmnopqrstuvwxyzabcRepository

/** The id arrives through assisted injection, at construction, from the navigation key. */
@HiltViewModel(assistedFactory = AbcdefghijklmnopqrstuvwxyzabcDetailViewModel.Factory::class)
internal class AbcdefghijklmnopqrstuvwxyzabcDetailViewModel @AssistedInject constructor(
    private val repository: AbcdefghijklmnopqrstuvwxyzabcRepository,
    @Assisted private val id: String,
) : MviViewModel<
    AbcdefghijklmnopqrstuvwxyzabcDetailState,
    AbcdefghijklmnopqrstuvwxyzabcDetailEvent,
    AbcdefghijklmnopqrstuvwxyzabcDetailEffect,
    >(
    initial = AbcdefghijklmnopqrstuvwxyzabcDetailState(),
) {

    @AssistedFactory
    interface Factory {
        fun create(id: String): AbcdefghijklmnopqrstuvwxyzabcDetailViewModel
    }

    override fun handleEvent(event: AbcdefghijklmnopqrstuvwxyzabcDetailEvent) {
        when (event) {
            AbcdefghijklmnopqrstuvwxyzabcDetailEvent.Appeared -> {
                if (current.loadState == LoadState.Idle) load()
            }

            AbcdefghijklmnopqrstuvwxyzabcDetailEvent.Retry -> load()

            AbcdefghijklmnopqrstuvwxyzabcDetailEvent.BackClicked -> {
                emit(AbcdefghijklmnopqrstuvwxyzabcDetailEffect.NavigateBack)
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
