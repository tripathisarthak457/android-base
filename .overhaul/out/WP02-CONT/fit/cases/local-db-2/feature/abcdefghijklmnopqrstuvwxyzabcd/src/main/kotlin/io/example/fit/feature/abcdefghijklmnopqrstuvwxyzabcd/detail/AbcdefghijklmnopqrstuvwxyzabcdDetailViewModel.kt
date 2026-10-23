package io.example.fit.feature.abcdefghijklmnopqrstuvwxyzabcd.detail

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.example.fit.core.common.LoadState
import io.example.fit.core.common.result.AppResult
import io.example.fit.core.mvi.MviViewModel
import io.example.fit.data.abcdefghijklmnopqrstuvwxyzabcd.api.AbcdefghijklmnopqrstuvwxyzabcdRepository

/** The id arrives through assisted injection, at construction, from the navigation key. */
@HiltViewModel(assistedFactory = AbcdefghijklmnopqrstuvwxyzabcdDetailViewModel.Factory::class)
internal class AbcdefghijklmnopqrstuvwxyzabcdDetailViewModel @AssistedInject constructor(
    private val repository: AbcdefghijklmnopqrstuvwxyzabcdRepository,
    @Assisted private val id: String,
) : MviViewModel<
    AbcdefghijklmnopqrstuvwxyzabcdDetailState,
    AbcdefghijklmnopqrstuvwxyzabcdDetailEvent,
    AbcdefghijklmnopqrstuvwxyzabcdDetailEffect,
    >(
    initial = AbcdefghijklmnopqrstuvwxyzabcdDetailState(),
) {

    @AssistedFactory
    interface Factory {
        fun create(id: String): AbcdefghijklmnopqrstuvwxyzabcdDetailViewModel
    }

    override fun handleEvent(event: AbcdefghijklmnopqrstuvwxyzabcdDetailEvent) {
        when (event) {
            AbcdefghijklmnopqrstuvwxyzabcdDetailEvent.Appeared -> {
                if (current.loadState == LoadState.Idle) load()
            }

            AbcdefghijklmnopqrstuvwxyzabcdDetailEvent.Retry -> load()

            AbcdefghijklmnopqrstuvwxyzabcdDetailEvent.BackClicked -> {
                emit(AbcdefghijklmnopqrstuvwxyzabcdDetailEffect.NavigateBack)
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
