package io.example.fit.feature.abcdefghijklmnopqrstuvwxyzabc.list

import androidx.compose.runtime.Immutable
import io.example.fit.core.common.LoadState
import io.example.fit.core.mvi.UiEffect
import io.example.fit.core.mvi.UiEvent
import io.example.fit.core.mvi.UiState
import io.example.fit.data.abcdefghijklmnopqrstuvwxyzabc.api.model.AbcdefghijklmnopqrstuvwxyzabcItem

@Immutable
internal data class AbcdefghijklmnopqrstuvwxyzabcListState(
    val loadState: LoadState = LoadState.Idle,
    val items: List<AbcdefghijklmnopqrstuvwxyzabcItem> = emptyList(),
) : UiState

internal sealed interface AbcdefghijklmnopqrstuvwxyzabcListEvent : UiEvent {
    data object Appeared : AbcdefghijklmnopqrstuvwxyzabcListEvent
    data object Refresh : AbcdefghijklmnopqrstuvwxyzabcListEvent
    data object Retry : AbcdefghijklmnopqrstuvwxyzabcListEvent
    data class ItemClicked(val id: String) : AbcdefghijklmnopqrstuvwxyzabcListEvent
}

internal sealed interface AbcdefghijklmnopqrstuvwxyzabcListEffect : UiEffect {
    data class OpenDetail(val id: String) : AbcdefghijklmnopqrstuvwxyzabcListEffect
}
