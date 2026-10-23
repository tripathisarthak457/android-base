package io.example.fit.feature.abcdefghijklmnopqrst.detail

import androidx.compose.runtime.Immutable
import io.example.fit.core.common.LoadState
import io.example.fit.core.mvi.UiEffect
import io.example.fit.core.mvi.UiEvent
import io.example.fit.core.mvi.UiState
import io.example.fit.data.abcdefghijklmnopqrst.api.model.AbcdefghijklmnopqrstItem

@Immutable
internal data class AbcdefghijklmnopqrstDetailState(
    val loadState: LoadState = LoadState.Idle,
    val item: AbcdefghijklmnopqrstItem? = null,
) : UiState

internal sealed interface AbcdefghijklmnopqrstDetailEvent : UiEvent {
    data object Appeared : AbcdefghijklmnopqrstDetailEvent
    data object Retry : AbcdefghijklmnopqrstDetailEvent
    data object BackClicked : AbcdefghijklmnopqrstDetailEvent
}

internal sealed interface AbcdefghijklmnopqrstDetailEffect : UiEffect {
    data object NavigateBack : AbcdefghijklmnopqrstDetailEffect
}
