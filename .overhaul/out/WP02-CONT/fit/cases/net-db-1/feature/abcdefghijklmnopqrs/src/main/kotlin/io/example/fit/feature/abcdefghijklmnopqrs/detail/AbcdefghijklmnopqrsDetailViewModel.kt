package io.example.fit.feature.abcdefghijklmnopqrs.detail

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.example.fit.core.common.LoadState
import io.example.fit.core.common.result.AppResult
import io.example.fit.core.mvi.MviViewModel
import io.example.fit.data.abcdefghijklmnopqrs.api.AbcdefghijklmnopqrsRepository

/** The id arrives through assisted injection, at construction, from the navigation key. */
@HiltViewModel(assistedFactory = AbcdefghijklmnopqrsDetailViewModel.Factory::class)
internal class AbcdefghijklmnopqrsDetailViewModel @AssistedInject constructor(
    private val repository: AbcdefghijklmnopqrsRepository,
    @Assisted private val id: String,
) : MviViewModel<
    AbcdefghijklmnopqrsDetailState,
    AbcdefghijklmnopqrsDetailEvent,
    AbcdefghijklmnopqrsDetailEffect,
    >(
    initial = AbcdefghijklmnopqrsDetailState(),
) {

    @AssistedFactory
    interface Factory {
        fun create(id: String): AbcdefghijklmnopqrsDetailViewModel
    }

    override fun handleEvent(event: AbcdefghijklmnopqrsDetailEvent) {
        when (event) {
            AbcdefghijklmnopqrsDetailEvent.Appeared -> {
                if (current.loadState == LoadState.Idle) load()
            }

            AbcdefghijklmnopqrsDetailEvent.Retry -> load()

            AbcdefghijklmnopqrsDetailEvent.BackClicked -> {
                emit(AbcdefghijklmnopqrsDetailEffect.NavigateBack)
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
