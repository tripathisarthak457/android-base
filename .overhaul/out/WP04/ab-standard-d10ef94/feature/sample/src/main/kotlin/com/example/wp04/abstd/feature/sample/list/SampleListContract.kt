package com.example.wp04.abstd.feature.sample.list

import androidx.compose.runtime.Immutable
import com.example.wp04.abstd.core.common.LoadState
import com.example.wp04.abstd.core.mvi.UiEffect
import com.example.wp04.abstd.core.mvi.UiEvent
import com.example.wp04.abstd.core.mvi.UiState
import com.example.wp04.abstd.data.sample.api.SampleItem

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
    data object Appeared : SampleListEvent
    data object Refresh : SampleListEvent
    data object Retry : SampleListEvent
    data class ItemClicked(val id: Int) : SampleListEvent
}

internal sealed interface SampleListEffect : UiEffect {
    data class OpenDetail(val id: Int) : SampleListEffect
}
