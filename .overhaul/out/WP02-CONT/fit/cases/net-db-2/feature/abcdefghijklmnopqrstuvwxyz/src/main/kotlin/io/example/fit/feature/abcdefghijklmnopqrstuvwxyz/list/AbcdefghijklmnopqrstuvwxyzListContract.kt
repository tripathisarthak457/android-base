package io.example.fit.feature.abcdefghijklmnopqrstuvwxyz.list

import androidx.compose.runtime.Immutable
import io.example.fit.core.common.LoadState
import io.example.fit.core.mvi.UiEffect
import io.example.fit.core.mvi.UiEvent
import io.example.fit.core.mvi.UiState
import io.example.fit.data.abcdefghijklmnopqrstuvwxyz.api.model.AbcdefghijklmnopqrstuvwxyzItem

@Immutable
internal data class AbcdefghijklmnopqrstuvwxyzListState(
    val loadState: LoadState = LoadState.Idle,
    val items: List<AbcdefghijklmnopqrstuvwxyzItem> = emptyList(),
) : UiState

internal sealed interface AbcdefghijklmnopqrstuvwxyzListEvent : UiEvent {
    data object Appeared : AbcdefghijklmnopqrstuvwxyzListEvent
    data object Refresh : AbcdefghijklmnopqrstuvwxyzListEvent
    data object Retry : AbcdefghijklmnopqrstuvwxyzListEvent
    data class ItemClicked(val id: String) : AbcdefghijklmnopqrstuvwxyzListEvent
}

internal sealed interface AbcdefghijklmnopqrstuvwxyzListEffect : UiEffect {
    data class OpenDetail(val id: String) : AbcdefghijklmnopqrstuvwxyzListEffect
}
