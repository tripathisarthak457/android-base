package io.example.fitclocal.feature.abcdefghijklmno.list

import androidx.compose.runtime.Immutable
import io.example.fitclocal.core.common.LoadState
import io.example.fitclocal.core.mvi.UiEffect
import io.example.fitclocal.core.mvi.UiEvent
import io.example.fitclocal.core.mvi.UiState
import io.example.fitclocal.data.abcdefghijklmno.api.model.AbcdefghijklmnoItem

@Immutable
internal data class AbcdefghijklmnoListState(
    val loadState: LoadState = LoadState.Idle,
    val items: List<AbcdefghijklmnoItem> = emptyList(),
) : UiState

internal sealed interface AbcdefghijklmnoListEvent : UiEvent {
    data object Appeared : AbcdefghijklmnoListEvent
    data object Refresh : AbcdefghijklmnoListEvent
    data object Retry : AbcdefghijklmnoListEvent
    data class ItemClicked(val id: String) : AbcdefghijklmnoListEvent
}

internal sealed interface AbcdefghijklmnoListEffect : UiEffect {
    data class OpenDetail(val id: String) : AbcdefghijklmnoListEffect
}
