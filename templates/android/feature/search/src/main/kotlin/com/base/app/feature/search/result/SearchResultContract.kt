package com.base.app.feature.search.result

import androidx.compose.runtime.Immutable
import com.base.app.core.common.LoadState
import com.base.app.core.mvi.UiEffect
import com.base.app.core.mvi.UiEvent
import com.base.app.core.mvi.UiState
import com.base.app.data.search.api.model.SearchResult

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
