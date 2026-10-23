package io.example.fit.feature.abcdefghijklmnopqrstuvwx.list

import androidx.compose.runtime.Immutable
import io.example.fit.core.common.LoadState
import io.example.fit.core.mvi.UiEffect
import io.example.fit.core.mvi.UiEvent
import io.example.fit.core.mvi.UiState
import io.example.fit.data.abcdefghijklmnopqrstuvwx.api.model.AbcdefghijklmnopqrstuvwxItem

@Immutable
internal data class AbcdefghijklmnopqrstuvwxListState(
    val loadState: LoadState = LoadState.Idle,
    val items: List<AbcdefghijklmnopqrstuvwxItem> = emptyList(),
) : UiState

internal sealed interface AbcdefghijklmnopqrstuvwxListEvent : UiEvent {
    data object Appeared : AbcdefghijklmnopqrstuvwxListEvent
    data object Refresh : AbcdefghijklmnopqrstuvwxListEvent
    data object Retry : AbcdefghijklmnopqrstuvwxListEvent
    data class ItemClicked(val id: String) : AbcdefghijklmnopqrstuvwxListEvent
}

internal sealed interface AbcdefghijklmnopqrstuvwxListEffect : UiEffect {
    data class OpenDetail(val id: String) : AbcdefghijklmnopqrstuvwxListEffect
}
