package io.example.fitcnetdb.feature.abc.list

import androidx.compose.runtime.Immutable
import io.example.fitcnetdb.core.common.LoadState
import io.example.fitcnetdb.core.mvi.UiEffect
import io.example.fitcnetdb.core.mvi.UiEvent
import io.example.fitcnetdb.core.mvi.UiState
import io.example.fitcnetdb.data.abc.api.model.AbcItem

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
