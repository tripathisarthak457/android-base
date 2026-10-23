package io.example.fit.feature.abcdefghijklmnopqrst.detail

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.example.fit.core.common.LoadState
import io.example.fit.core.common.result.AppResult
import io.example.fit.core.mvi.MviViewModel
import io.example.fit.data.abcdefghijklmnopqrst.api.AbcdefghijklmnopqrstRepository

/** The id arrives through assisted injection, at construction, from the navigation key. */
@HiltViewModel(assistedFactory = AbcdefghijklmnopqrstDetailViewModel.Factory::class)
internal class AbcdefghijklmnopqrstDetailViewModel @AssistedInject constructor(
    private val repository: AbcdefghijklmnopqrstRepository,
    @Assisted private val id: String,
) : MviViewModel<
    AbcdefghijklmnopqrstDetailState,
    AbcdefghijklmnopqrstDetailEvent,
    AbcdefghijklmnopqrstDetailEffect,
    >(
    initial = AbcdefghijklmnopqrstDetailState(),
) {

    @AssistedFactory
    interface Factory {
        fun create(id: String): AbcdefghijklmnopqrstDetailViewModel
    }

    override fun handleEvent(event: AbcdefghijklmnopqrstDetailEvent) {
        when (event) {
            AbcdefghijklmnopqrstDetailEvent.Appeared -> {
                if (current.loadState == LoadState.Idle) load()
            }

            AbcdefghijklmnopqrstDetailEvent.Retry -> load()

            AbcdefghijklmnopqrstDetailEvent.BackClicked -> {
                emit(AbcdefghijklmnopqrstDetailEffect.NavigateBack)
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
