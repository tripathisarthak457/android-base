package io.example.fit.feature.a.detail

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.example.fit.core.common.LoadState
import io.example.fit.core.common.result.AppResult
import io.example.fit.core.mvi.MviViewModel
import io.example.fit.data.a.api.ARepository

/** The id arrives through assisted injection, at construction, from the navigation key. */
@HiltViewModel(assistedFactory = ADetailViewModel.Factory::class)
internal class ADetailViewModel @AssistedInject constructor(
    private val repository: ARepository,
    @Assisted private val id: String,
) : MviViewModel<ADetailState, ADetailEvent, ADetailEffect>(
    initial = ADetailState(),
) {

    @AssistedFactory
    interface Factory {
        fun create(id: String): ADetailViewModel
    }

    override fun handleEvent(event: ADetailEvent) {
        when (event) {
            ADetailEvent.Appeared -> {
                if (current.loadState == LoadState.Idle) load()
            }

            ADetailEvent.Retry -> load()

            ADetailEvent.BackClicked -> {
                emit(ADetailEffect.NavigateBack)
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
