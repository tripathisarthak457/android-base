package io.example.fit.feature.abcdefghij.list

import androidx.compose.runtime.Immutable
import io.example.fit.core.common.LoadState
import io.example.fit.core.mvi.UiEffect
import io.example.fit.core.mvi.UiEvent
import io.example.fit.core.mvi.UiState
import io.example.fit.data.abcdefghij.api.model.AbcdefghijItem

@Immutable
internal data class AbcdefghijListState(
    val loadState: LoadState = LoadState.Idle,
    val items: List<AbcdefghijItem> = emptyList(),
) : UiState

internal sealed interface AbcdefghijListEvent : UiEvent {
    data object Appeared : AbcdefghijListEvent
    data object Refresh : AbcdefghijListEvent
    data object Retry : AbcdefghijListEvent
    data class ItemClicked(val id: String) : AbcdefghijListEvent
}

internal sealed interface AbcdefghijListEffect : UiEffect {
    data class OpenDetail(val id: String) : AbcdefghijListEffect
}
