package io.example.fit.feature.abcdefg.detail

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.example.fit.core.common.LoadState
import io.example.fit.core.common.result.AppResult
import io.example.fit.core.mvi.MviViewModel
import io.example.fit.data.abcdefg.api.AbcdefgRepository

/** The id arrives through assisted injection, at construction, from the navigation key. */
@HiltViewModel(assistedFactory = AbcdefgDetailViewModel.Factory::class)
internal class AbcdefgDetailViewModel @AssistedInject constructor(
    private val repository: AbcdefgRepository,
    @Assisted private val id: String,
) : MviViewModel<AbcdefgDetailState, AbcdefgDetailEvent, AbcdefgDetailEffect>(
    initial = AbcdefgDetailState(),
) {

    @AssistedFactory
    interface Factory {
        fun create(id: String): AbcdefgDetailViewModel
    }

    override fun handleEvent(event: AbcdefgDetailEvent) {
        when (event) {
            AbcdefgDetailEvent.Appeared -> {
                if (current.loadState == LoadState.Idle) load()
            }

            AbcdefgDetailEvent.Retry -> load()

            AbcdefgDetailEvent.BackClicked -> {
                emit(AbcdefgDetailEffect.NavigateBack)
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
