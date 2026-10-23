package io.example.fit.feature.abcdefghijkl.list

import androidx.compose.runtime.Immutable
import io.example.fit.core.common.LoadState
import io.example.fit.core.mvi.UiEffect
import io.example.fit.core.mvi.UiEvent
import io.example.fit.core.mvi.UiState
import io.example.fit.data.abcdefghijkl.api.model.AbcdefghijklItem

@Immutable
internal data class AbcdefghijklListState(
    val loadState: LoadState = LoadState.Idle,
    val items: List<AbcdefghijklItem> = emptyList(),
) : UiState

internal sealed interface AbcdefghijklListEvent : UiEvent {
    data object Appeared : AbcdefghijklListEvent
    data object Refresh : AbcdefghijklListEvent
    data object Retry : AbcdefghijklListEvent
    data class ItemClicked(val id: String) : AbcdefghijklListEvent
}

internal sealed interface AbcdefghijklListEffect : UiEffect {
    data class OpenDetail(val id: String) : AbcdefghijklListEffect
}
