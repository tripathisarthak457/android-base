package com.base.app.feature.sample.list

import com.base.app.core.common.AppResult
import com.base.app.core.common.mvi.LoadState
import com.base.app.core.common.mvi.MessageKind
import com.base.app.core.common.mvi.MviViewModel
import com.base.app.core.common.util.asUiText
import com.base.app.data.sample.SampleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * The reference ViewModel. Every feature in this project is shaped like this one.
 *
 * ## The initial load runs from `init`
 *
 * Rather than from a `LaunchedEffect` in the composable. A `LaunchedEffect(Unit)` re-runs whenever
 * the composable leaves and re-enters the composition — a tab switch, a configuration change on
 * some paths — and re-fetches a list the ViewModel already has. `init` runs exactly once per
 * ViewModel, which is exactly once per screen instance.
 *
 * ## Refresh keeps the content on screen
 *
 * The distinction between [LoadState.Loading] and [LoadState.Refreshing] is why pull-to-refresh
 * here does not blank the list it is refreshing. It costs one `if` and it is the difference
 * between a refresh that feels instant and one that feels like a reload.
 */
@HiltViewModel
class SampleListViewModel @Inject constructor(
    private val repository: SampleRepository,
) : MviViewModel<SampleListState, SampleListEvent, SampleListEffect>(SampleListState()) {

    init {
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
                // A failed *refresh* keeps whatever is already on screen and says so in a
                // snackbar. Replacing a good list with a full-screen error because a background
                // refresh failed is the most annoying possible response to a flaky network.
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
