package io.example.fitclocal.feature.abc.list

import androidx.compose.runtime.Immutable
import io.example.fitclocal.core.common.LoadState
import io.example.fitclocal.core.mvi.UiEffect
import io.example.fitclocal.core.mvi.UiEvent
import io.example.fitclocal.core.mvi.UiState
import io.example.fitclocal.data.abc.api.model.AbcItem

@Immutable
internal data class AbcListState(
    val loadState: LoadState = LoadState.Idle,
    val items: List<AbcItem> = emptyList(),
) : UiState

internal sealed interface AbcListEvent : UiEvent {
    data object Appeared : AbcListEvent
    data object Refresh : AbcListEvent
    data object Retry : AbcListEvent
    data class ItemClicked(val id: String) : AbcListEvent
}

internal sealed interface AbcListEffect : UiEffect {
    data class OpenDetail(val id: String) : AbcListEffect
}
