package io.example.fit.feature.ab.detail

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.example.fit.core.common.LoadState
import io.example.fit.core.common.result.AppResult
import io.example.fit.core.mvi.MviViewModel
import io.example.fit.data.ab.api.AbRepository

/** The id arrives through assisted injection, at construction, from the navigation key. */
@HiltViewModel(assistedFactory = AbDetailViewModel.Factory::class)
internal class AbDetailViewModel @AssistedInject constructor(
    private val repository: AbRepository,
    @Assisted private val id: String,
) : MviViewModel<AbDetailState, AbDetailEvent, AbDetailEffect>(
    initial = AbDetailState(),
) {

    @AssistedFactory
    interface Factory {
        fun create(id: String): AbDetailViewModel
    }

    override fun handleEvent(event: AbDetailEvent) {
        when (event) {
            AbDetailEvent.Appeared -> {
                if (current.loadState == LoadState.Idle) load()
            }

            AbDetailEvent.Retry -> load()

            AbDetailEvent.BackClicked -> {
                emit(AbDetailEffect.NavigateBack)
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
