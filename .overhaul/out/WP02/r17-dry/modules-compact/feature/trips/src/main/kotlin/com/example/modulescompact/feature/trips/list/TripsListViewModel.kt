package com.example.modulescompact.feature.trips.list

import com.example.modulescompact.core.common.LoadState
import com.example.modulescompact.core.common.result.AppResult
import com.example.modulescompact.core.mvi.MessageKind
import com.example.modulescompact.core.mvi.MviViewModel
import com.example.modulescompact.core.ui.toUiText
import com.example.modulescompact.data.trips.api.TripsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
internal class TripsListViewModel @Inject constructor(private val repository: TripsRepository) :
    MviViewModel<TripsListState, TripsListEvent, TripsListEffect>(
        initial = TripsListState(),
    ) {

    override fun handleEvent(event: TripsListEvent) {
        when (event) {
            TripsListEvent.Appeared -> {
                if (current.loadState == LoadState.Idle) load(refreshing = false)
            }

            TripsListEvent.Refresh -> load(refreshing = true)

            TripsListEvent.Retry -> load(refreshing = false)

            is TripsListEvent.ItemClicked -> {
                emit(TripsListEffect.OpenDetail(event.id))
            }
        }
    }

    private fun load(refreshing: Boolean) {
        launchLatest(LOAD) {
            val loading = if (refreshing) LoadState.Refreshing else LoadState.Loading
            setState { copy(loadState = loading) }

            when (val result = repository.items(forceRefresh = refreshing)) {
                is AppResult.Success -> setState {
                    val loaded = if (result.value.isEmpty()) LoadState.Empty else LoadState.Success
                    copy(loadState = loaded, items = result.value)
                }

                // A failed refresh keeps the rows on screen and says so in a message.
                is AppResult.Failure -> if (refreshing && current.items.isNotEmpty()) {
                    setState { copy(loadState = LoadState.Success) }
                    showMessage(text = result.error.toUiText(), kind = MessageKind.Error)
                } else {
                    setState { copy(loadState = LoadState.Error(result.error)) }
                }
            }
        }
    }

    private companion object {
        const val LOAD = "load"
    }
}
