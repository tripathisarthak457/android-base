package io.example.fit.feature.abcdefghijklmnopqrstu.detail

import androidx.compose.runtime.Immutable
import io.example.fit.core.common.LoadState
import io.example.fit.core.mvi.UiEffect
import io.example.fit.core.mvi.UiEvent
import io.example.fit.core.mvi.UiState
import io.example.fit.data.abcdefghijklmnopqrstu.api.model.AbcdefghijklmnopqrstuItem

@Immutable
internal data class AbcdefghijklmnopqrstuDetailState(
    val loadState: LoadState = LoadState.Idle,
    val item: AbcdefghijklmnopqrstuItem? = null,
) : UiState

internal sealed interface AbcdefghijklmnopqrstuDetailEvent : UiEvent {
    data object Appeared : AbcdefghijklmnopqrstuDetailEvent
    data object Retry : AbcdefghijklmnopqrstuDetailEvent
    data object BackClicked : AbcdefghijklmnopqrstuDetailEvent
}

internal sealed interface AbcdefghijklmnopqrstuDetailEffect : UiEffect {
    data object NavigateBack : AbcdefghijklmnopqrstuDetailEffect
}
