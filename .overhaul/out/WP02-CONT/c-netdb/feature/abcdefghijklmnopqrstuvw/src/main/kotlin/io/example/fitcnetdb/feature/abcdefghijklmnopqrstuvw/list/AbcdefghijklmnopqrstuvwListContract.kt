package io.example.fitcnetdb.feature.abcdefghijklmnopqrstuvw.list

import androidx.compose.runtime.Immutable
import io.example.fitcnetdb.core.common.LoadState
import io.example.fitcnetdb.core.mvi.UiEffect
import io.example.fitcnetdb.core.mvi.UiEvent
import io.example.fitcnetdb.core.mvi.UiState
import io.example.fitcnetdb.data.abcdefghijklmnopqrstuvw.api.model.AbcdefghijklmnopqrstuvwItem

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
