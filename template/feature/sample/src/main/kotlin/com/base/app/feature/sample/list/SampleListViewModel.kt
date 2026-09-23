package com.base.app.feature.sample.list

import com.base.app.core.common.AppResult
import com.base.app.core.common.mvi.LoadState
import com.base.app.core.common.mvi.MessageKind
import com.base.app.core.common.mvi.MviViewModel
import com.base.app.core.common.util.asUiText
import androidx.lifecycle.SavedStateHandle
import com.base.app.data.sample.SampleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** The reference ViewModel. Every feature in this project is shaped like this one. */
@HiltViewModel
class SampleListViewModel @Inject constructor(
    private val repository: SampleRepository,
    savedStateHandle: SavedStateHandle,
) : MviViewModel<SampleListState, SampleListEvent, SampleListEffect>(SampleListState()) {

    init {
        // What the user typed survives the process being killed; the list itself does not,
        // because it is a request away and a restored one would be an hour stale.
        persistState(
            handle = savedStateHandle,
            save = { mapOf("query" to it.query) },
            restore = { copy(query = it["query"] as? String ?: query) },
        )
        onEvent(SampleListEvent.Load)
    }

    override suspend fun handleEvent(event: SampleListEvent) {
        when (event) {
            SampleListEvent.Load -> load(refreshing = false)
            SampleListEvent.Refresh -> load(refreshing = true)
            SampleListEvent.Retry -> load(refreshing = false)
            is SampleListEvent.QueryChanged -> updateState { copy(query = event.query) }
            is SampleListEvent.ItemClicked -> emitEffect(SampleListEffect.OpenDetail(event.id))
        }
    }

    private suspend fun load(refreshing: Boolean) {
        updateState {
            copy(loadState = if (refreshing) LoadState.Refreshing else LoadState.Loading)
        }

        when (val result = repository.items(forceRefresh = refreshing)) {
            is AppResult.Success -> updateState {
                copy(
                    loadState = if (result.data.isEmpty()) LoadState.Empty else LoadState.Success,
                    items = result.data,
                    isFromCache = result.fromCache,
                )
            }

            is AppResult.Failure -> {
                // A failed refresh keeps the list on screen and reports it in a snackbar.
                if (refreshing && currentState.items.isNotEmpty()) {
                    updateState { copy(loadState = LoadState.Success) }
                    showMessage(
                        text = (result.message ?: "Could not refresh.").asUiText(),
                        kind = MessageKind.Error,
                    )
                } else {
                    updateState { copy(loadState = result.toLoadState()) }
                }
            }
        }
    }
}
