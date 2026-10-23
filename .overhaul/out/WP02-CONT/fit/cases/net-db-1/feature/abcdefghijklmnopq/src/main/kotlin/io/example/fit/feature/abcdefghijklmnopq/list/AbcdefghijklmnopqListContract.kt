package io.example.fit.feature.abcdefghijklmnopq.list

import androidx.compose.runtime.Immutable
import io.example.fit.core.common.LoadState
import io.example.fit.core.mvi.UiEffect
import io.example.fit.core.mvi.UiEvent
import io.example.fit.core.mvi.UiState
import io.example.fit.data.abcdefghijklmnopq.api.model.AbcdefghijklmnopqItem

@Immutable
internal data class AbcdefghijklmnopqListState(
    val loadState: LoadState = LoadState.Idle,
    val items: List<AbcdefghijklmnopqItem> = emptyList(),
) : UiState

internal sealed interface AbcdefghijklmnopqListEvent : UiEvent {
    data object Appeared : AbcdefghijklmnopqListEvent
    data object Refresh : AbcdefghijklmnopqListEvent
    data object Retry : AbcdefghijklmnopqListEvent
    data class ItemClicked(val id: String) : AbcdefghijklmnopqListEvent
}

internal sealed interface AbcdefghijklmnopqListEffect : UiEffect {
    data class OpenDetail(val id: String) : AbcdefghijklmnopqListEffect
}
