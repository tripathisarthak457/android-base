package io.example.fit.feature.a.list

import androidx.compose.runtime.Immutable
import io.example.fit.core.common.LoadState
import io.example.fit.core.mvi.UiEffect
import io.example.fit.core.mvi.UiEvent
import io.example.fit.core.mvi.UiState
import io.example.fit.data.a.api.model.AItem

@Immutable
internal data class AListState(
    val loadState: LoadState = LoadState.Idle,
    val items: List<AItem> = emptyList(),
) : UiState

internal sealed interface AListEvent : UiEvent {
    data object Appeared : AListEvent
    data object Refresh : AListEvent
    data object Retry : AListEvent
    data class ItemClicked(val id: String) : AListEvent
}

internal sealed interface AListEffect : UiEffect {
    data class OpenDetail(val id: String) : AListEffect
}
