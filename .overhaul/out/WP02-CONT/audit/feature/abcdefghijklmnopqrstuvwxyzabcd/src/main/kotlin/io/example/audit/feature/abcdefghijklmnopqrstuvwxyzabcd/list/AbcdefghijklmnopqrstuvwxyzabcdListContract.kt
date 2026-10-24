package io.example.audit.feature.abcdefghijklmnopqrstuvwxyzabcd.list

import androidx.compose.runtime.Immutable
import io.example.audit.core.common.LoadState
import io.example.audit.core.mvi.UiEffect
import io.example.audit.core.mvi.UiEvent
import io.example.audit.core.mvi.UiState
import io.example.audit.data.abcdefghijklmnopqrstuvwxyzabcd.api.model.AbcdefghijklmnopqrstuvwxyzabcdItem

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
