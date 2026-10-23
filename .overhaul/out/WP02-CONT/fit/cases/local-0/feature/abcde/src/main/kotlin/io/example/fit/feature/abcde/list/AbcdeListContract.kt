package io.example.fit.feature.abcde.list

import androidx.compose.runtime.Immutable
import io.example.fit.core.common.LoadState
import io.example.fit.core.mvi.UiEffect
import io.example.fit.core.mvi.UiEvent
import io.example.fit.core.mvi.UiState
import io.example.fit.data.abcde.api.model.AbcdeItem

@Immutable
internal data class AbcdeListState(
    val loadState: LoadState = LoadState.Idle,
    val items: List<AbcdeItem> = emptyList(),
) : UiState

internal sealed interface AbcdeListEvent : UiEvent {
    data object Appeared : AbcdeListEvent
    data object Refresh : AbcdeListEvent
    data object Retry : AbcdeListEvent
    data class ItemClicked(val id: String) : AbcdeListEvent
}

internal sealed interface AbcdeListEffect : UiEffect {
    data class OpenDetail(val id: String) : AbcdeListEffect
}
