package io.example.fitcnetdb.feature.abcdefghijklmnopqrs.list

import androidx.compose.runtime.Immutable
import io.example.fitcnetdb.core.common.LoadState
import io.example.fitcnetdb.core.mvi.UiEffect
import io.example.fitcnetdb.core.mvi.UiEvent
import io.example.fitcnetdb.core.mvi.UiState
import io.example.fitcnetdb.data.abcdefghijklmnopqrs.api.model.AbcdefghijklmnopqrsItem

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
