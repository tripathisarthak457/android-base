package com.example.finalafter.crnoui3.feature.search.result

import androidx.compose.runtime.Immutable
import com.example.finalafter.crnoui3.core.common.LoadState
import com.example.finalafter.crnoui3.core.mvi.UiEffect
import com.example.finalafter.crnoui3.core.mvi.UiEvent
import com.example.finalafter.crnoui3.core.mvi.UiState
import com.example.finalafter.crnoui3.data.search.api.model.SearchResult

@Immutable
internal data class SearchResultState(
    val loadState: LoadState = LoadState.Idle,
    val result: SearchResult? = null,
) : UiState

internal sealed interface SearchResultEvent : UiEvent {
    data object Retry : SearchResultEvent
    data object BackClicked : SearchResultEvent
}

internal sealed interface SearchResultEffect : UiEffect {
    data object NavigateBack : SearchResultEffect
}
