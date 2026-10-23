package io.example.fit.feature.abcdefghijklmnopqr.detail

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.example.fit.core.common.LoadState
import io.example.fit.core.common.result.AppResult
import io.example.fit.core.mvi.MviViewModel
import io.example.fit.data.abcdefghijklmnopqr.api.AbcdefghijklmnopqrRepository

/** The id arrives through assisted injection, at construction, from the navigation key. */
@HiltViewModel(assistedFactory = AbcdefghijklmnopqrDetailViewModel.Factory::class)
internal class AbcdefghijklmnopqrDetailViewModel @AssistedInject constructor(
    private val repository: AbcdefghijklmnopqrRepository,
    @Assisted private val id: String,
) : MviViewModel<
    AbcdefghijklmnopqrDetailState,
    AbcdefghijklmnopqrDetailEvent,
    AbcdefghijklmnopqrDetailEffect,
    >(
    initial = AbcdefghijklmnopqrDetailState(),
) {

    @AssistedFactory
    interface Factory {
        fun create(id: String): AbcdefghijklmnopqrDetailViewModel
    }

    override fun handleEvent(event: AbcdefghijklmnopqrDetailEvent) {
        when (event) {
            AbcdefghijklmnopqrDetailEvent.Appeared -> {
                if (current.loadState == LoadState.Idle) load()
            }

            AbcdefghijklmnopqrDetailEvent.Retry -> load()

            AbcdefghijklmnopqrDetailEvent.BackClicked -> {
                emit(AbcdefghijklmnopqrDetailEffect.NavigateBack)
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
