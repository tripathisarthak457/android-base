package io.example.fitcnet.feature.abcdefghi.detail

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.example.fitcnet.core.common.LoadState
import io.example.fitcnet.core.common.result.AppResult
import io.example.fitcnet.core.mvi.MviViewModel
import io.example.fitcnet.data.abcdefghi.api.AbcdefghiRepository

/** The id arrives through assisted injection, at construction, from the navigation key. */
@HiltViewModel(assistedFactory = AbcdefghiDetailViewModel.Factory::class)
internal class AbcdefghiDetailViewModel @AssistedInject constructor(
    private val repository: AbcdefghiRepository,
    @Assisted private val id: String,
) : MviViewModel<AbcdefghiDetailState, AbcdefghiDetailEvent, AbcdefghiDetailEffect>(
    initial = AbcdefghiDetailState(),
) {

    @AssistedFactory
    interface Factory {
        fun create(id: String): AbcdefghiDetailViewModel
    }

    override fun handleEvent(event: AbcdefghiDetailEvent) {
        when (event) {
            AbcdefghiDetailEvent.Appeared -> {
                if (current.loadState == LoadState.Idle) load()
            }

            AbcdefghiDetailEvent.Retry -> load()

            AbcdefghiDetailEvent.BackClicked -> {
                emit(AbcdefghiDetailEffect.NavigateBack)
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
