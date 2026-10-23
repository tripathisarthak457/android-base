package io.example.fit.feature.abcdefghijk.list

import androidx.compose.runtime.Immutable
import io.example.fit.core.common.LoadState
import io.example.fit.core.mvi.UiEffect
import io.example.fit.core.mvi.UiEvent
import io.example.fit.core.mvi.UiState
import io.example.fit.data.abcdefghijk.api.model.AbcdefghijkItem

@Immutable
internal data class AbcdefghijkListState(
    val loadState: LoadState = LoadState.Idle,
    val items: List<AbcdefghijkItem> = emptyList(),
) : UiState

internal sealed interface AbcdefghijkListEvent : UiEvent {
    data object Appeared : AbcdefghijkListEvent
    data object Refresh : AbcdefghijkListEvent
    data object Retry : AbcdefghijkListEvent
    data class ItemClicked(val id: String) : AbcdefghijkListEvent
}

internal sealed interface AbcdefghijkListEffect : UiEffect {
    data class OpenDetail(val id: String) : AbcdefghijkListEffect
}
