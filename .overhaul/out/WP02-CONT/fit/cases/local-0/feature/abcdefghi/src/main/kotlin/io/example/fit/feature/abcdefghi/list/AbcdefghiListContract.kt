package io.example.fit.feature.abcdefghi.list

import androidx.compose.runtime.Immutable
import io.example.fit.core.common.LoadState
import io.example.fit.core.mvi.UiEffect
import io.example.fit.core.mvi.UiEvent
import io.example.fit.core.mvi.UiState
import io.example.fit.data.abcdefghi.api.model.AbcdefghiItem

@Immutable
internal data class AbcdefghiListState(
    val loadState: LoadState = LoadState.Idle,
    val items: List<AbcdefghiItem> = emptyList(),
) : UiState

internal sealed interface AbcdefghiListEvent : UiEvent {
    data object Appeared : AbcdefghiListEvent
    data object Refresh : AbcdefghiListEvent
    data object Retry : AbcdefghiListEvent
    data class ItemClicked(val id: String) : AbcdefghiListEvent
}

internal sealed interface AbcdefghiListEffect : UiEffect {
    data class OpenDetail(val id: String) : AbcdefghiListEffect
}
