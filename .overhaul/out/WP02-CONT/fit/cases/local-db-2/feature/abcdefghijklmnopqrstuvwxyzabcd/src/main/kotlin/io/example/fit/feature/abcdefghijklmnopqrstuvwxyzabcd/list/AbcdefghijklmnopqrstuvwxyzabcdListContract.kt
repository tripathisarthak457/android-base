package io.example.fit.feature.abcdefghijklmnopqrstuvwxyzabcd.list

import androidx.compose.runtime.Immutable
import io.example.fit.core.common.LoadState
import io.example.fit.core.mvi.UiEffect
import io.example.fit.core.mvi.UiEvent
import io.example.fit.core.mvi.UiState
import io.example.fit.data.abcdefghijklmnopqrstuvwxyzabcd.api.model.AbcdefghijklmnopqrstuvwxyzabcdItem

@Immutable
internal data class AbcdefghijklmnopqrstuvwxyzabcdListState(
    val loadState: LoadState = LoadState.Idle,
    val items: List<AbcdefghijklmnopqrstuvwxyzabcdItem> = emptyList(),
) : UiState

internal sealed interface AbcdefghijklmnopqrstuvwxyzabcdListEvent : UiEvent {
    data object Appeared : AbcdefghijklmnopqrstuvwxyzabcdListEvent
    data object Refresh : AbcdefghijklmnopqrstuvwxyzabcdListEvent
    data object Retry : AbcdefghijklmnopqrstuvwxyzabcdListEvent
    data class ItemClicked(val id: String) : AbcdefghijklmnopqrstuvwxyzabcdListEvent
}

internal sealed interface AbcdefghijklmnopqrstuvwxyzabcdListEffect : UiEffect {
    data class OpenDetail(val id: String) : AbcdefghijklmnopqrstuvwxyzabcdListEffect
}
