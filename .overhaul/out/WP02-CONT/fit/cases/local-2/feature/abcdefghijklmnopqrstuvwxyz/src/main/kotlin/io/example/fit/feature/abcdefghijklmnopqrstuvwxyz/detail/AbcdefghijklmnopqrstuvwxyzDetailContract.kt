package io.example.fit.feature.abcdefghijklmnopqrstuvwxyz.detail

import androidx.compose.runtime.Immutable
import io.example.fit.core.common.LoadState
import io.example.fit.core.mvi.UiEffect
import io.example.fit.core.mvi.UiEvent
import io.example.fit.core.mvi.UiState
import io.example.fit.data.abcdefghijklmnopqrstuvwxyz.api.model.AbcdefghijklmnopqrstuvwxyzItem

@Immutable
internal data class AbcdefghijklmnopqrstuvwxyzDetailState(
    val loadState: LoadState = LoadState.Idle,
    val item: AbcdefghijklmnopqrstuvwxyzItem? = null,
) : UiState

internal sealed interface AbcdefghijklmnopqrstuvwxyzDetailEvent : UiEvent {
    data object Appeared : AbcdefghijklmnopqrstuvwxyzDetailEvent
    data object Retry : AbcdefghijklmnopqrstuvwxyzDetailEvent
    data object BackClicked : AbcdefghijklmnopqrstuvwxyzDetailEvent
}

internal sealed interface AbcdefghijklmnopqrstuvwxyzDetailEffect : UiEffect {
    data object NavigateBack : AbcdefghijklmnopqrstuvwxyzDetailEffect
}
