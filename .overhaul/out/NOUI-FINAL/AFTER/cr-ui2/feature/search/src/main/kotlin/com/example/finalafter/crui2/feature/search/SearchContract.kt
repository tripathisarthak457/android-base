package com.example.finalafter.crui2.feature.search

import androidx.compose.runtime.Immutable
import com.example.finalafter.crui2.core.common.LoadState
import com.example.finalafter.crui2.core.mvi.UiEffect
import com.example.finalafter.crui2.core.mvi.UiEvent
import com.example.finalafter.crui2.core.mvi.UiState
import com.example.finalafter.crui2.data.search.api.model.SearchResult

/** The field's text is not here: it lives in the ViewModel's `query`. */
@Immutable
internal data class SearchState(
    /** The query the results answer. It trails the field by the debounce. */
    val searchedFor: String = "",
    val loadState: LoadState = LoadState.Idle,
    val results: List<SearchResult> = emptyList(),
    val recent: List<String> = emptyList(),
) : UiState

internal sealed interface SearchEvent : UiEvent {
    data object Submit : SearchEvent
    data object Retry : SearchEvent
    data class RecentClicked(val query: String) : SearchEvent
    data class RecentRemoved(val query: String) : SearchEvent
    data object ClearRecent : SearchEvent
    data class ResultClicked(val result: SearchResult) : SearchEvent
}

internal sealed interface SearchEffect : UiEffect {
    data class OpenResult(val id: Int) : SearchEffect
}
