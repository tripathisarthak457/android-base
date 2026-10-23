package io.example.fit.feature.abcdefghijklmnop.list

import androidx.compose.runtime.Immutable
import io.example.fit.core.common.LoadState
import io.example.fit.core.mvi.UiEffect
import io.example.fit.core.mvi.UiEvent
import io.example.fit.core.mvi.UiState
import io.example.fit.data.abcdefghijklmnop.api.model.AbcdefghijklmnopItem

@Immutable
internal data class AbcdefghijklmnopListState(
    val loadState: LoadState = LoadState.Idle,
    val items: List<AbcdefghijklmnopItem> = emptyList(),
) : UiState

internal sealed interface AbcdefghijklmnopListEvent : UiEvent {
    data object Appeared : AbcdefghijklmnopListEvent
    data object Refresh : AbcdefghijklmnopListEvent
    data object Retry : AbcdefghijklmnopListEvent
    data class ItemClicked(val id: String) : AbcdefghijklmnopListEvent
}

internal sealed interface AbcdefghijklmnopListEffect : UiEffect {
    data class OpenDetail(val id: String) : AbcdefghijklmnopListEffect
}
