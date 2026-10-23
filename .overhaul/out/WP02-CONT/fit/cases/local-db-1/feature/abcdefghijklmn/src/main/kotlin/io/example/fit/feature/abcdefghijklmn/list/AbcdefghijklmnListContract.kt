package io.example.fit.feature.abcdefghijklmn.list

import androidx.compose.runtime.Immutable
import io.example.fit.core.common.LoadState
import io.example.fit.core.mvi.UiEffect
import io.example.fit.core.mvi.UiEvent
import io.example.fit.core.mvi.UiState
import io.example.fit.data.abcdefghijklmn.api.model.AbcdefghijklmnItem

@Immutable
internal data class AbcdefghijklmnListState(
    val loadState: LoadState = LoadState.Idle,
    val items: List<AbcdefghijklmnItem> = emptyList(),
) : UiState

internal sealed interface AbcdefghijklmnListEvent : UiEvent {
    data object Appeared : AbcdefghijklmnListEvent
    data object Refresh : AbcdefghijklmnListEvent
    data object Retry : AbcdefghijklmnListEvent
    data class ItemClicked(val id: String) : AbcdefghijklmnListEvent
}

internal sealed interface AbcdefghijklmnListEffect : UiEffect {
    data class OpenDetail(val id: String) : AbcdefghijklmnListEffect
}
