package com.base.app.feature.sample.list

import androidx.compose.runtime.Immutable
import com.base.app.core.common.mvi.LoadState
import com.base.app.core.common.mvi.UiEffect
import com.base.app.core.common.mvi.UiEvent
import com.base.app.core.common.mvi.UiState
import com.base.app.data.sample.SampleItem

/** Everything the list screen renders. */
@Immutable
data class SampleListState(
    val loadState: LoadState = LoadState.Idle,
    val items: List<SampleItem> = emptyList(),
    val query: String = "",
    val isFromCache: Boolean = false,
) : UiState {

    /** Filtering lives in the state, not in the composable. */
    val visibleItems: List<SampleItem>
        get() = if (query.isBlank()) {
            items
        } else {
            items.filter { it.title.contains(query, ignoreCase = true) }
        }
}

sealed interface SampleListEvent : UiEvent {
    data object Load : SampleListEvent
    data object Refresh : SampleListEvent
    data object Retry : SampleListEvent
    data class QueryChanged(val query: String) : SampleListEvent
    data class ItemClicked(val id: Int) : SampleListEvent
}

sealed interface SampleListEffect : UiEffect {
    data class OpenDetail(val id: Int) : SampleListEffect
}
