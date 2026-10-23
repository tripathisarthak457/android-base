package io.example.fit.feature.abcdefgh.list

import androidx.compose.runtime.Immutable
import io.example.fit.core.common.LoadState
import io.example.fit.core.mvi.UiEffect
import io.example.fit.core.mvi.UiEvent
import io.example.fit.core.mvi.UiState
import io.example.fit.data.abcdefgh.api.model.AbcdefghItem

@Immutable
internal data class AbcdefghListState(
    val loadState: LoadState = LoadState.Idle,
    val items: List<AbcdefghItem> = emptyList(),
) : UiState

internal sealed interface AbcdefghListEvent : UiEvent {
    data object Appeared : AbcdefghListEvent
    data object Refresh : AbcdefghListEvent
    data object Retry : AbcdefghListEvent
    data class ItemClicked(val id: String) : AbcdefghListEvent
}

internal sealed interface AbcdefghListEffect : UiEffect {
    data class OpenDetail(val id: String) : AbcdefghListEffect
}
