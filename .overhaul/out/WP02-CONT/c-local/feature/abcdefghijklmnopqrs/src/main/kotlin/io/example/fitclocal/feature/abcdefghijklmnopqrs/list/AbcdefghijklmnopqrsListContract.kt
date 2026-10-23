package io.example.fitclocal.feature.abcdefghijklmnopqrs.list

import androidx.compose.runtime.Immutable
import io.example.fitclocal.core.common.LoadState
import io.example.fitclocal.core.mvi.UiEffect
import io.example.fitclocal.core.mvi.UiEvent
import io.example.fitclocal.core.mvi.UiState
import io.example.fitclocal.data.abcdefghijklmnopqrs.api.model.AbcdefghijklmnopqrsItem

@Immutable
internal data class AbcdefghijklmnopqrsListState(
    val loadState: LoadState = LoadState.Idle,
    val items: List<AbcdefghijklmnopqrsItem> = emptyList(),
) : UiState

internal sealed interface AbcdefghijklmnopqrsListEvent : UiEvent {
    data object Appeared : AbcdefghijklmnopqrsListEvent
    data object Refresh : AbcdefghijklmnopqrsListEvent
    data object Retry : AbcdefghijklmnopqrsListEvent
    data class ItemClicked(val id: String) : AbcdefghijklmnopqrsListEvent
}

internal sealed interface AbcdefghijklmnopqrsListEffect : UiEffect {
    data class OpenDetail(val id: String) : AbcdefghijklmnopqrsListEffect
}
