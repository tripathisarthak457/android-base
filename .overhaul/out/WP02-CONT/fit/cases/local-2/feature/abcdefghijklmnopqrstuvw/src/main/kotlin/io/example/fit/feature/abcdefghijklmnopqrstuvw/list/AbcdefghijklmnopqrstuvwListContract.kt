package io.example.fit.feature.abcdefghijklmnopqrstuvw.list

import androidx.compose.runtime.Immutable
import io.example.fit.core.common.LoadState
import io.example.fit.core.mvi.UiEffect
import io.example.fit.core.mvi.UiEvent
import io.example.fit.core.mvi.UiState
import io.example.fit.data.abcdefghijklmnopqrstuvw.api.model.AbcdefghijklmnopqrstuvwItem

@Immutable
internal data class AbcdefghijklmnopqrstuvwListState(
    val loadState: LoadState = LoadState.Idle,
    val items: List<AbcdefghijklmnopqrstuvwItem> = emptyList(),
) : UiState

internal sealed interface AbcdefghijklmnopqrstuvwListEvent : UiEvent {
    data object Appeared : AbcdefghijklmnopqrstuvwListEvent
    data object Refresh : AbcdefghijklmnopqrstuvwListEvent
    data object Retry : AbcdefghijklmnopqrstuvwListEvent
    data class ItemClicked(val id: String) : AbcdefghijklmnopqrstuvwListEvent
}

internal sealed interface AbcdefghijklmnopqrstuvwListEffect : UiEffect {
    data class OpenDetail(val id: String) : AbcdefghijklmnopqrstuvwListEffect
}
