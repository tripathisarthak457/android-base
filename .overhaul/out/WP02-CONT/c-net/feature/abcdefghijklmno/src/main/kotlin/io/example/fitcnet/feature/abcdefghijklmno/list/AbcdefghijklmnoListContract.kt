package io.example.fitcnet.feature.abcdefghijklmno.list

import androidx.compose.runtime.Immutable
import io.example.fitcnet.core.common.LoadState
import io.example.fitcnet.core.mvi.UiEffect
import io.example.fitcnet.core.mvi.UiEvent
import io.example.fitcnet.core.mvi.UiState
import io.example.fitcnet.data.abcdefghijklmno.api.model.AbcdefghijklmnoItem

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
