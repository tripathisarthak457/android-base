package io.example.fit.feature.abcdefghijklmnopqrst.list

import androidx.compose.runtime.Immutable
import io.example.fit.core.common.LoadState
import io.example.fit.core.mvi.UiEffect
import io.example.fit.core.mvi.UiEvent
import io.example.fit.core.mvi.UiState
import io.example.fit.data.abcdefghijklmnopqrst.api.model.AbcdefghijklmnopqrstItem

@Immutable
internal data class AbcdefghijklmnopqrstListState(
    val loadState: LoadState = LoadState.Idle,
    val items: List<AbcdefghijklmnopqrstItem> = emptyList(),
) : UiState

internal sealed interface AbcdefghijklmnopqrstListEvent : UiEvent {
    data object Appeared : AbcdefghijklmnopqrstListEvent
    data object Refresh : AbcdefghijklmnopqrstListEvent
    data object Retry : AbcdefghijklmnopqrstListEvent
    data class ItemClicked(val id: String) : AbcdefghijklmnopqrstListEvent
}

internal sealed interface AbcdefghijklmnopqrstListEffect : UiEffect {
    data class OpenDetail(val id: String) : AbcdefghijklmnopqrstListEffect
}
