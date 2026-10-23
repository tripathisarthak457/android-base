package io.example.fit.feature.abcdefghijklmnop.detail

import androidx.compose.runtime.Immutable
import io.example.fit.core.common.LoadState
import io.example.fit.core.mvi.UiEffect
import io.example.fit.core.mvi.UiEvent
import io.example.fit.core.mvi.UiState
import io.example.fit.data.abcdefghijklmnop.api.model.AbcdefghijklmnopItem

@Immutable
internal data class AbcdefghijklmnopDetailState(
    val loadState: LoadState = LoadState.Idle,
    val item: AbcdefghijklmnopItem? = null,
) : UiState

internal sealed interface AbcdefghijklmnopDetailEvent : UiEvent {
    data object Appeared : AbcdefghijklmnopDetailEvent
    data object Retry : AbcdefghijklmnopDetailEvent
    data object BackClicked : AbcdefghijklmnopDetailEvent
}

internal sealed interface AbcdefghijklmnopDetailEffect : UiEffect {
    data object NavigateBack : AbcdefghijklmnopDetailEffect
}
