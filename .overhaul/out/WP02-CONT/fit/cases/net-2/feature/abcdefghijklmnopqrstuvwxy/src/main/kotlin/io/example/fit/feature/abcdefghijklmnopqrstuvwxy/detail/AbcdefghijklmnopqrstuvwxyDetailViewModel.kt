package io.example.fit.feature.abcdefghijklmnopqrstuvwxy.detail

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.example.fit.core.common.LoadState
import io.example.fit.core.common.result.AppResult
import io.example.fit.core.mvi.MviViewModel
import io.example.fit.data.abcdefghijklmnopqrstuvwxy.api.AbcdefghijklmnopqrstuvwxyRepository

/** The id arrives through assisted injection, at construction, from the navigation key. */
@HiltViewModel(assistedFactory = AbcdefghijklmnopqrstuvwxyDetailViewModel.Factory::class)
internal class AbcdefghijklmnopqrstuvwxyDetailViewModel @AssistedInject constructor(
    private val repository: AbcdefghijklmnopqrstuvwxyRepository,
    @Assisted private val id: String,
) : MviViewModel<
    AbcdefghijklmnopqrstuvwxyDetailState,
    AbcdefghijklmnopqrstuvwxyDetailEvent,
    AbcdefghijklmnopqrstuvwxyDetailEffect,
    >(
    initial = AbcdefghijklmnopqrstuvwxyDetailState(),
) {

    @AssistedFactory
    interface Factory {
        fun create(id: String): AbcdefghijklmnopqrstuvwxyDetailViewModel
    }

    override fun handleEvent(event: AbcdefghijklmnopqrstuvwxyDetailEvent) {
        when (event) {
            AbcdefghijklmnopqrstuvwxyDetailEvent.Appeared -> {
                if (current.loadState == LoadState.Idle) load()
            }

            AbcdefghijklmnopqrstuvwxyDetailEvent.Retry -> load()

            AbcdefghijklmnopqrstuvwxyDetailEvent.BackClicked -> {
                emit(AbcdefghijklmnopqrstuvwxyDetailEffect.NavigateBack)
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
