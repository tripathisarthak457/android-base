package io.example.fit.feature.abcdef.list

import androidx.compose.runtime.Immutable
import io.example.fit.core.common.LoadState
import io.example.fit.core.mvi.UiEffect
import io.example.fit.core.mvi.UiEvent
import io.example.fit.core.mvi.UiState
import io.example.fit.data.abcdef.api.model.AbcdefItem

@Immutable
internal data class AbcdefListState(
    val loadState: LoadState = LoadState.Idle,
    val items: List<AbcdefItem> = emptyList(),
) : UiState

internal sealed interface AbcdefListEvent : UiEvent {
    data object Appeared : AbcdefListEvent
    data object Refresh : AbcdefListEvent
    data object Retry : AbcdefListEvent
    data class ItemClicked(val id: String) : AbcdefListEvent
}

internal sealed interface AbcdefListEffect : UiEffect {
    data class OpenDetail(val id: String) : AbcdefListEffect
}
