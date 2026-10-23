package io.example.fit.feature.abcdefghijklmnopqr.list

import androidx.compose.runtime.Immutable
import io.example.fit.core.common.LoadState
import io.example.fit.core.mvi.UiEffect
import io.example.fit.core.mvi.UiEvent
import io.example.fit.core.mvi.UiState
import io.example.fit.data.abcdefghijklmnopqr.api.model.AbcdefghijklmnopqrItem

@Immutable
internal data class AbcdefghijklmnopqrListState(
    val loadState: LoadState = LoadState.Idle,
    val items: List<AbcdefghijklmnopqrItem> = emptyList(),
) : UiState

internal sealed interface AbcdefghijklmnopqrListEvent : UiEvent {
    data object Appeared : AbcdefghijklmnopqrListEvent
    data object Refresh : AbcdefghijklmnopqrListEvent
    data object Retry : AbcdefghijklmnopqrListEvent
    data class ItemClicked(val id: String) : AbcdefghijklmnopqrListEvent
}

internal sealed interface AbcdefghijklmnopqrListEffect : UiEffect {
    data class OpenDetail(val id: String) : AbcdefghijklmnopqrListEffect
}
