package io.example.fit.feature.abcdefghijklm.list

import androidx.compose.runtime.Immutable
import io.example.fit.core.common.LoadState
import io.example.fit.core.mvi.UiEffect
import io.example.fit.core.mvi.UiEvent
import io.example.fit.core.mvi.UiState
import io.example.fit.data.abcdefghijklm.api.model.AbcdefghijklmItem

@Immutable
internal data class AbcdefghijklmListState(
    val loadState: LoadState = LoadState.Idle,
    val items: List<AbcdefghijklmItem> = emptyList(),
) : UiState

internal sealed interface AbcdefghijklmListEvent : UiEvent {
    data object Appeared : AbcdefghijklmListEvent
    data object Refresh : AbcdefghijklmListEvent
    data object Retry : AbcdefghijklmListEvent
    data class ItemClicked(val id: String) : AbcdefghijklmListEvent
}

internal sealed interface AbcdefghijklmListEffect : UiEffect {
    data class OpenDetail(val id: String) : AbcdefghijklmListEffect
}
