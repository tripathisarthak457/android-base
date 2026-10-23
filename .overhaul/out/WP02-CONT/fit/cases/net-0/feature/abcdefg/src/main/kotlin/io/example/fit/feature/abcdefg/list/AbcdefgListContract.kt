package io.example.fit.feature.abcdefg.list

import androidx.compose.runtime.Immutable
import io.example.fit.core.common.LoadState
import io.example.fit.core.mvi.UiEffect
import io.example.fit.core.mvi.UiEvent
import io.example.fit.core.mvi.UiState
import io.example.fit.data.abcdefg.api.model.AbcdefgItem

@Immutable
internal data class AbcdefgListState(
    val loadState: LoadState = LoadState.Idle,
    val items: List<AbcdefgItem> = emptyList(),
) : UiState

internal sealed interface AbcdefgListEvent : UiEvent {
    data object Appeared : AbcdefgListEvent
    data object Refresh : AbcdefgListEvent
    data object Retry : AbcdefgListEvent
    data class ItemClicked(val id: String) : AbcdefgListEvent
}

internal sealed interface AbcdefgListEffect : UiEffect {
    data class OpenDetail(val id: String) : AbcdefgListEffect
}
