package io.example.fit.feature.abcd.list

import androidx.compose.runtime.Immutable
import io.example.fit.core.common.LoadState
import io.example.fit.core.mvi.UiEffect
import io.example.fit.core.mvi.UiEvent
import io.example.fit.core.mvi.UiState
import io.example.fit.data.abcd.api.model.AbcdItem

@Immutable
internal data class AbcdListState(
    val loadState: LoadState = LoadState.Idle,
    val items: List<AbcdItem> = emptyList(),
) : UiState

internal sealed interface AbcdListEvent : UiEvent {
    data object Appeared : AbcdListEvent
    data object Refresh : AbcdListEvent
    data object Retry : AbcdListEvent
    data class ItemClicked(val id: String) : AbcdListEvent
}

internal sealed interface AbcdListEffect : UiEffect {
    data class OpenDetail(val id: String) : AbcdListEffect
}
