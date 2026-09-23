package com.base.app.feature.search

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import com.base.app.core.common.AppResult
import com.base.app.core.common.mvi.LoadState
import com.base.app.core.common.mvi.MviViewModel
import com.base.app.core.common.mvi.UiEffect
import com.base.app.core.common.mvi.UiEvent
import com.base.app.core.common.mvi.UiState
import com.base.app.data.search.SearchRepository
import com.base.app.data.search.SearchResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@Immutable
data class SearchState(
    val query: String = "",
    val loadState: LoadState = LoadState.Idle,
    val results: List<SearchResult> = emptyList(),
    val recent: List<String> = emptyList(),
) : UiState

sealed interface SearchEvent : UiEvent {
    data class QueryChanged(val query: String) : SearchEvent
    data object Submit : SearchEvent
    data object Retry : SearchEvent
    data class RecentClicked(val query: String) : SearchEvent
    data class RecentRemoved(val query: String) : SearchEvent
    data object ClearRecent : SearchEvent
    data class ResultClicked(val result: SearchResult) : SearchEvent
}

sealed interface SearchEffect : UiEffect {
    data class OpenResult(val id: Int) : SearchEffect
}

/** Search as you type. */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: SearchRepository,
    savedStateHandle: SavedStateHandle,
) : MviViewModel<SearchState, SearchEvent, SearchEffect>(SearchState()) {

    init {
        persistState(
            handle = savedStateHandle,
            save = { mapOf("query" to it.query) },
            restore = { copy(query = it["query"] as? String ?: query) },
        )
        launchWork { repository.recent.collect { recent -> updateState { copy(recent = recent) } } }
        if (currentState.query.isNotBlank()) search(currentState.query, debounce = false)
    }

    override suspend fun handleEvent(event: SearchEvent) {
        when (event) {
            is SearchEvent.QueryChanged -> {
                updateState { copy(query = event.query) }
                search(event.query, debounce = true)
            }

            SearchEvent.Submit -> {
                search(currentState.query, debounce = false)
                repository.remember(currentState.query)
            }

            SearchEvent.Retry -> search(currentState.query, debounce = false)

            is SearchEvent.RecentClicked -> {
                updateState { copy(query = event.query) }
                search(event.query, debounce = false)
                repository.remember(event.query)
            }

            is SearchEvent.RecentRemoved -> repository.forget(event.query)
            SearchEvent.ClearRecent -> repository.clearRecent()

            is SearchEvent.ResultClicked -> {
                repository.remember(currentState.query)
                emitEffect(SearchEffect.OpenResult(event.result.id))
            }
        }
    }

    private fun search(query: String, debounce: Boolean) {
        if (query.isBlank()) {
            launchLatest(SEARCH_JOB) { updateState { copy(loadState = LoadState.Idle, results = emptyList()) } }
            return
        }
        launchLatest(SEARCH_JOB, debounceMillis = if (debounce) DEBOUNCE_MILLIS else 0L) {
            // Keeps the previous results on screen while the next ones load, so typing does not
            // flash the list empty between every keystroke.
            updateState { copy(loadState = if (results.isEmpty()) LoadState.Loading else LoadState.Refreshing) }
            when (val result = repository.search(query)) {
                is AppResult.Success -> updateState {
                    copy(
                        results = result.data,
                        loadState = if (result.data.isEmpty()) LoadState.Empty else LoadState.Success,
                    )
                }

                is AppResult.Failure -> updateState { copy(loadState = result.toLoadState(), results = emptyList()) }
            }
        }
    }

    private companion object {
        const val SEARCH_JOB = "search"
        const val DEBOUNCE_MILLIS = 350L
    }
}
