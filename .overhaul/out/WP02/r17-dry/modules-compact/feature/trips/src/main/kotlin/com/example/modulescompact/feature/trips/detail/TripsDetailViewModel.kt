package com.example.modulescompact.feature.trips.detail

import com.example.modulescompact.core.common.LoadState
import com.example.modulescompact.core.common.result.AppResult
import com.example.modulescompact.core.mvi.MviViewModel
import com.example.modulescompact.data.trips.api.TripsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

/** The id arrives through assisted injection, at construction, from the navigation key. */
@HiltViewModel(assistedFactory = TripsDetailViewModel.Factory::class)
internal class TripsDetailViewModel @AssistedInject constructor(
    private val repository: TripsRepository,
    @Assisted private val id: String,
) : MviViewModel<TripsDetailState, TripsDetailEvent, TripsDetailEffect>(
    initial = TripsDetailState(),
) {

    @AssistedFactory
    interface Factory {
        fun create(id: String): TripsDetailViewModel
    }

    override fun handleEvent(event: TripsDetailEvent) {
        when (event) {
            TripsDetailEvent.Appeared -> {
                if (current.loadState == LoadState.Idle) load()
            }

            TripsDetailEvent.Retry -> load()

            TripsDetailEvent.BackClicked -> {
                emit(TripsDetailEffect.NavigateBack)
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
