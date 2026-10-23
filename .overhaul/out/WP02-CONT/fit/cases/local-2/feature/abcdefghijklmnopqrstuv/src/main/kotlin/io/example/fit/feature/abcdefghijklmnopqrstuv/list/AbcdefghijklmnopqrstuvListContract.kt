package io.example.fit.feature.abcdefghijklmnopqrstuv.list

import androidx.compose.runtime.Immutable
import io.example.fit.core.common.LoadState
import io.example.fit.core.mvi.UiEffect
import io.example.fit.core.mvi.UiEvent
import io.example.fit.core.mvi.UiState
import io.example.fit.data.abcdefghijklmnopqrstuv.api.model.AbcdefghijklmnopqrstuvItem

@Immutable
internal data class AbcdefghijklmnopqrstuvListState(
    val loadState: LoadState = LoadState.Idle,
    val items: List<AbcdefghijklmnopqrstuvItem> = emptyList(),
) : UiState

internal sealed interface AbcdefghijklmnopqrstuvListEvent : UiEvent {
    data object Appeared : AbcdefghijklmnopqrstuvListEvent
    data object Refresh : AbcdefghijklmnopqrstuvListEvent
    data object Retry : AbcdefghijklmnopqrstuvListEvent
    data class ItemClicked(val id: String) : AbcdefghijklmnopqrstuvListEvent
}

internal sealed interface AbcdefghijklmnopqrstuvListEffect : UiEffect {
    data class OpenDetail(val id: String) : AbcdefghijklmnopqrstuvListEffect
}
