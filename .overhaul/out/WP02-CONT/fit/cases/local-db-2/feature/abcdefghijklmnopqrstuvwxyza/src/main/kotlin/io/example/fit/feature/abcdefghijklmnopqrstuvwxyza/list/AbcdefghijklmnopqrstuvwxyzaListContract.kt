package io.example.fit.feature.abcdefghijklmnopqrstuvwxyza.list

import androidx.compose.runtime.Immutable
import io.example.fit.core.common.LoadState
import io.example.fit.core.mvi.UiEffect
import io.example.fit.core.mvi.UiEvent
import io.example.fit.core.mvi.UiState
import io.example.fit.data.abcdefghijklmnopqrstuvwxyza.api.model.AbcdefghijklmnopqrstuvwxyzaItem

@Immutable
internal data class AbcdefghijklmnopqrstuvwxyzaListState(
    val loadState: LoadState = LoadState.Idle,
    val items: List<AbcdefghijklmnopqrstuvwxyzaItem> = emptyList(),
) : UiState

internal sealed interface AbcdefghijklmnopqrstuvwxyzaListEvent : UiEvent {
    data object Appeared : AbcdefghijklmnopqrstuvwxyzaListEvent
    data object Refresh : AbcdefghijklmnopqrstuvwxyzaListEvent
    data object Retry : AbcdefghijklmnopqrstuvwxyzaListEvent
    data class ItemClicked(val id: String) : AbcdefghijklmnopqrstuvwxyzaListEvent
}

internal sealed interface AbcdefghijklmnopqrstuvwxyzaListEffect : UiEffect {
    data class OpenDetail(val id: String) : AbcdefghijklmnopqrstuvwxyzaListEffect
}
