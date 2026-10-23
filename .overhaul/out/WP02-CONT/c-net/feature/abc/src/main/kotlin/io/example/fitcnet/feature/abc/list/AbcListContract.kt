package io.example.fitcnet.feature.abc.list

import androidx.compose.runtime.Immutable
import io.example.fitcnet.core.common.LoadState
import io.example.fitcnet.core.mvi.UiEffect
import io.example.fitcnet.core.mvi.UiEvent
import io.example.fitcnet.core.mvi.UiState
import io.example.fitcnet.data.abc.api.model.AbcItem

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
