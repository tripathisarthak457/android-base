package io.example.fit.feature.abcdef.detail

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.example.fit.core.common.LoadState
import io.example.fit.core.common.result.AppResult
import io.example.fit.core.mvi.MviViewModel
import io.example.fit.data.abcdef.api.AbcdefRepository

/** The id arrives through assisted injection, at construction, from the navigation key. */
@HiltViewModel(assistedFactory = AbcdefDetailViewModel.Factory::class)
internal class AbcdefDetailViewModel @AssistedInject constructor(
    private val repository: AbcdefRepository,
    @Assisted private val id: String,
) : MviViewModel<AbcdefDetailState, AbcdefDetailEvent, AbcdefDetailEffect>(
    initial = AbcdefDetailState(),
) {

    @AssistedFactory
    interface Factory {
        fun create(id: String): AbcdefDetailViewModel
    }

    override fun handleEvent(event: AbcdefDetailEvent) {
        when (event) {
            AbcdefDetailEvent.Appeared -> {
                if (current.loadState == LoadState.Idle) load()
            }

            AbcdefDetailEvent.Retry -> load()

            AbcdefDetailEvent.BackClicked -> {
                emit(AbcdefDetailEffect.NavigateBack)
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
