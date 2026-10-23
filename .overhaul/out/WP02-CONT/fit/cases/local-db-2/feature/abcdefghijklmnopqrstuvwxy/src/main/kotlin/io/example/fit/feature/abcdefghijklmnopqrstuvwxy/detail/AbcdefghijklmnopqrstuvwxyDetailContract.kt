package io.example.fit.feature.abcdefghijklmnopqrstuvwxy.detail

import androidx.compose.runtime.Immutable
import io.example.fit.core.common.LoadState
import io.example.fit.core.mvi.UiEffect
import io.example.fit.core.mvi.UiEvent
import io.example.fit.core.mvi.UiState
import io.example.fit.data.abcdefghijklmnopqrstuvwxy.api.model.AbcdefghijklmnopqrstuvwxyItem

@Immutable
internal data class AbcdefghijklmnopqrstuvwxyDetailState(
    val loadState: LoadState = LoadState.Idle,
    val item: AbcdefghijklmnopqrstuvwxyItem? = null,
) : UiState

internal sealed interface AbcdefghijklmnopqrstuvwxyDetailEvent : UiEvent {
    data object Appeared : AbcdefghijklmnopqrstuvwxyDetailEvent
    data object Retry : AbcdefghijklmnopqrstuvwxyDetailEvent
    data object BackClicked : AbcdefghijklmnopqrstuvwxyDetailEvent
}

internal sealed interface AbcdefghijklmnopqrstuvwxyDetailEffect : UiEffect {
    data object NavigateBack : AbcdefghijklmnopqrstuvwxyDetailEffect
}
