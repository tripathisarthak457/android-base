package io.example.fit.feature.abcdefghijklmnopqrstuvwxy.list

import androidx.compose.runtime.Immutable
import io.example.fit.core.common.LoadState
import io.example.fit.core.mvi.UiEffect
import io.example.fit.core.mvi.UiEvent
import io.example.fit.core.mvi.UiState
import io.example.fit.data.abcdefghijklmnopqrstuvwxy.api.model.AbcdefghijklmnopqrstuvwxyItem

@Immutable
internal data class AbcdefghijklmnopqrstuvwxyListState(
    val loadState: LoadState = LoadState.Idle,
    val items: List<AbcdefghijklmnopqrstuvwxyItem> = emptyList(),
) : UiState

internal sealed interface AbcdefghijklmnopqrstuvwxyListEvent : UiEvent {
    data object Appeared : AbcdefghijklmnopqrstuvwxyListEvent
    data object Refresh : AbcdefghijklmnopqrstuvwxyListEvent
    data object Retry : AbcdefghijklmnopqrstuvwxyListEvent
    data class ItemClicked(val id: String) : AbcdefghijklmnopqrstuvwxyListEvent
}

internal sealed interface AbcdefghijklmnopqrstuvwxyListEffect : UiEffect {
    data class OpenDetail(val id: String) : AbcdefghijklmnopqrstuvwxyListEffect
}
