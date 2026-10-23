package io.example.fit.feature.abcd.detail

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.example.fit.core.common.LoadState
import io.example.fit.core.common.result.AppResult
import io.example.fit.core.mvi.MviViewModel
import io.example.fit.data.abcd.api.AbcdRepository

/** The id arrives through assisted injection, at construction, from the navigation key. */
@HiltViewModel(assistedFactory = AbcdDetailViewModel.Factory::class)
internal class AbcdDetailViewModel @AssistedInject constructor(
    private val repository: AbcdRepository,
    @Assisted private val id: String,
) : MviViewModel<AbcdDetailState, AbcdDetailEvent, AbcdDetailEffect>(
    initial = AbcdDetailState(),
) {

    @AssistedFactory
    interface Factory {
        fun create(id: String): AbcdDetailViewModel
    }

    override fun handleEvent(event: AbcdDetailEvent) {
        when (event) {
            AbcdDetailEvent.Appeared -> {
                if (current.loadState == LoadState.Idle) load()
            }

            AbcdDetailEvent.Retry -> load()

            AbcdDetailEvent.BackClicked -> {
                emit(AbcdDetailEffect.NavigateBack)
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
