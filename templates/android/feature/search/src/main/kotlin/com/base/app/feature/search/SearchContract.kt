package com.base.app.feature.search

import androidx.compose.runtime.Immutable
import com.base.app.core.common.LoadState
import com.base.app.core.mvi.UiEffect
import com.base.app.core.mvi.UiEvent
import com.base.app.core.mvi.UiState
import com.base.app.data.search.api.model.SearchResult

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
