package io.example.fit.feature.abcdefghijklmnopqrstu.detail

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.example.fit.core.common.LoadState
import io.example.fit.core.common.result.AppResult
import io.example.fit.core.mvi.MviViewModel
import io.example.fit.data.abcdefghijklmnopqrstu.api.AbcdefghijklmnopqrstuRepository

/** The id arrives through assisted injection, at construction, from the navigation key. */
@HiltViewModel(assistedFactory = AbcdefghijklmnopqrstuDetailViewModel.Factory::class)
internal class AbcdefghijklmnopqrstuDetailViewModel @AssistedInject constructor(
    private val repository: AbcdefghijklmnopqrstuRepository,
    @Assisted private val id: String,
) : MviViewModel<
    AbcdefghijklmnopqrstuDetailState,
    AbcdefghijklmnopqrstuDetailEvent,
    AbcdefghijklmnopqrstuDetailEffect,
    >(
    initial = AbcdefghijklmnopqrstuDetailState(),
) {

    @AssistedFactory
    interface Factory {
        fun create(id: String): AbcdefghijklmnopqrstuDetailViewModel
    }

    override fun handleEvent(event: AbcdefghijklmnopqrstuDetailEvent) {
        when (event) {
            AbcdefghijklmnopqrstuDetailEvent.Appeared -> {
                if (current.loadState == LoadState.Idle) load()
            }

            AbcdefghijklmnopqrstuDetailEvent.Retry -> load()

            AbcdefghijklmnopqrstuDetailEvent.BackClicked -> {
                emit(AbcdefghijklmnopqrstuDetailEffect.NavigateBack)
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
