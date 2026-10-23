package io.example.fit.feature.abcdefghijklmnop.detail

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.example.fit.core.common.LoadState
import io.example.fit.core.common.result.AppResult
import io.example.fit.core.mvi.MviViewModel
import io.example.fit.data.abcdefghijklmnop.api.AbcdefghijklmnopRepository

/** The id arrives through assisted injection, at construction, from the navigation key. */
@HiltViewModel(assistedFactory = AbcdefghijklmnopDetailViewModel.Factory::class)
internal class AbcdefghijklmnopDetailViewModel @AssistedInject constructor(
    private val repository: AbcdefghijklmnopRepository,
    @Assisted private val id: String,
) : MviViewModel<
    AbcdefghijklmnopDetailState,
    AbcdefghijklmnopDetailEvent,
    AbcdefghijklmnopDetailEffect,
    >(
    initial = AbcdefghijklmnopDetailState(),
) {

    @AssistedFactory
    interface Factory {
        fun create(id: String): AbcdefghijklmnopDetailViewModel
    }

    override fun handleEvent(event: AbcdefghijklmnopDetailEvent) {
        when (event) {
            AbcdefghijklmnopDetailEvent.Appeared -> {
                if (current.loadState == LoadState.Idle) load()
            }

            AbcdefghijklmnopDetailEvent.Retry -> load()

            AbcdefghijklmnopDetailEvent.BackClicked -> {
                emit(AbcdefghijklmnopDetailEffect.NavigateBack)
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
