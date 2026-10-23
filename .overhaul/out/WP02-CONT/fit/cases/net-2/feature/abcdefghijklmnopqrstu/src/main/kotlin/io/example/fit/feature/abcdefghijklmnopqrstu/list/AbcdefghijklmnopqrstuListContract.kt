package io.example.fit.feature.abcdefghijklmnopqrstu.list

import androidx.compose.runtime.Immutable
import io.example.fit.core.common.LoadState
import io.example.fit.core.mvi.UiEffect
import io.example.fit.core.mvi.UiEvent
import io.example.fit.core.mvi.UiState
import io.example.fit.data.abcdefghijklmnopqrstu.api.model.AbcdefghijklmnopqrstuItem

@Immutable
internal data class AbcdefghijklmnopqrstuListState(
    val loadState: LoadState = LoadState.Idle,
    val items: List<AbcdefghijklmnopqrstuItem> = emptyList(),
) : UiState

internal sealed interface AbcdefghijklmnopqrstuListEvent : UiEvent {
    data object Appeared : AbcdefghijklmnopqrstuListEvent
    data object Refresh : AbcdefghijklmnopqrstuListEvent
    data object Retry : AbcdefghijklmnopqrstuListEvent
    data class ItemClicked(val id: String) : AbcdefghijklmnopqrstuListEvent
}

internal sealed interface AbcdefghijklmnopqrstuListEffect : UiEffect {
    data class OpenDetail(val id: String) : AbcdefghijklmnopqrstuListEffect
}
