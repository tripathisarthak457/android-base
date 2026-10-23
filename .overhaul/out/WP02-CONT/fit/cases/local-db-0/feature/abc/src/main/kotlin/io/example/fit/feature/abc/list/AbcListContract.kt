package io.example.fit.feature.abc.list

import androidx.compose.runtime.Immutable
import io.example.fit.core.common.LoadState
import io.example.fit.core.mvi.UiEffect
import io.example.fit.core.mvi.UiEvent
import io.example.fit.core.mvi.UiState
import io.example.fit.data.abc.api.model.AbcItem

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
