package io.example.fit.feature.abcdefghijklmnopqrstuvwxyzab.list

import androidx.compose.runtime.Immutable
import io.example.fit.core.common.LoadState
import io.example.fit.core.mvi.UiEffect
import io.example.fit.core.mvi.UiEvent
import io.example.fit.core.mvi.UiState
import io.example.fit.data.abcdefghijklmnopqrstuvwxyzab.api.model.AbcdefghijklmnopqrstuvwxyzabItem

@Immutable
internal data class AbcdefghijklmnopqrstuvwxyzabListState(
    val loadState: LoadState = LoadState.Idle,
    val items: List<AbcdefghijklmnopqrstuvwxyzabItem> = emptyList(),
) : UiState

internal sealed interface AbcdefghijklmnopqrstuvwxyzabListEvent : UiEvent {
    data object Appeared : AbcdefghijklmnopqrstuvwxyzabListEvent
    data object Refresh : AbcdefghijklmnopqrstuvwxyzabListEvent
    data object Retry : AbcdefghijklmnopqrstuvwxyzabListEvent
    data class ItemClicked(val id: String) : AbcdefghijklmnopqrstuvwxyzabListEvent
}

internal sealed interface AbcdefghijklmnopqrstuvwxyzabListEffect : UiEffect {
    data class OpenDetail(val id: String) : AbcdefghijklmnopqrstuvwxyzabListEffect
}
