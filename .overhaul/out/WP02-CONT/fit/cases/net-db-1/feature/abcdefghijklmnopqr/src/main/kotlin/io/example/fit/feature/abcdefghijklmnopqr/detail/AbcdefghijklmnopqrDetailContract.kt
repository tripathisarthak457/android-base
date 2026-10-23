package io.example.fit.feature.abcdefghijklmnopqr.detail

import androidx.compose.runtime.Immutable
import io.example.fit.core.common.LoadState
import io.example.fit.core.mvi.UiEffect
import io.example.fit.core.mvi.UiEvent
import io.example.fit.core.mvi.UiState
import io.example.fit.data.abcdefghijklmnopqr.api.model.AbcdefghijklmnopqrItem

@Immutable
internal data class AbcdefghijklmnopqrDetailState(
    val loadState: LoadState = LoadState.Idle,
    val item: AbcdefghijklmnopqrItem? = null,
) : UiState

internal sealed interface AbcdefghijklmnopqrDetailEvent : UiEvent {
    data object Appeared : AbcdefghijklmnopqrDetailEvent
    data object Retry : AbcdefghijklmnopqrDetailEvent
    data object BackClicked : AbcdefghijklmnopqrDetailEvent
}

internal sealed interface AbcdefghijklmnopqrDetailEffect : UiEffect {
    data object NavigateBack : AbcdefghijklmnopqrDetailEffect
}
