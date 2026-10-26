package com.example.finalafter.crui.feature.sample.list

import androidx.compose.runtime.Immutable
import com.example.finalafter.crui.core.common.LoadState
import com.example.finalafter.crui.core.mvi.UiEffect
import com.example.finalafter.crui.core.mvi.UiEvent
import com.example.finalafter.crui.core.mvi.UiState
import com.example.finalafter.crui.data.sample.api.model.SampleItem

/**
 * Everything the list screen renders. The search text is not here: it lives in the ViewModel's
 * `query` field, and [visibleItems] is what the reducer derived from it.
 */
@Immutable
internal data class SampleListState(
    val loadState: LoadState = LoadState.Idle,
    val items: List<SampleItem> = emptyList(),
    val visibleItems: List<SampleItem> = emptyList(),
) : UiState

internal sealed interface SampleListEvent : UiEvent {
    data object Refresh : SampleListEvent
    data object Retry : SampleListEvent
    data class ItemClicked(val id: Int) : SampleListEvent
}

internal sealed interface SampleListEffect : UiEffect {
    data class OpenDetail(val id: Int) : SampleListEffect
}
