package io.example.fit.feature.abcdefghijklmn.detail

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.example.fit.core.common.LoadState
import io.example.fit.core.common.result.AppResult
import io.example.fit.core.mvi.MviViewModel
import io.example.fit.data.abcdefghijklmn.api.AbcdefghijklmnRepository

/** The id arrives through assisted injection, at construction, from the navigation key. */
@HiltViewModel(assistedFactory = AbcdefghijklmnDetailViewModel.Factory::class)
internal class AbcdefghijklmnDetailViewModel @AssistedInject constructor(
    private val repository: AbcdefghijklmnRepository,
    @Assisted private val id: String,
) : MviViewModel<AbcdefghijklmnDetailState, AbcdefghijklmnDetailEvent, AbcdefghijklmnDetailEffect>(
    initial = AbcdefghijklmnDetailState(),
) {

    @AssistedFactory
    interface Factory {
        fun create(id: String): AbcdefghijklmnDetailViewModel
    }

    override fun handleEvent(event: AbcdefghijklmnDetailEvent) {
        when (event) {
            AbcdefghijklmnDetailEvent.Appeared -> {
                if (current.loadState == LoadState.Idle) load()
            }

            AbcdefghijklmnDetailEvent.Retry -> load()

            AbcdefghijklmnDetailEvent.BackClicked -> {
                emit(AbcdefghijklmnDetailEffect.NavigateBack)
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
