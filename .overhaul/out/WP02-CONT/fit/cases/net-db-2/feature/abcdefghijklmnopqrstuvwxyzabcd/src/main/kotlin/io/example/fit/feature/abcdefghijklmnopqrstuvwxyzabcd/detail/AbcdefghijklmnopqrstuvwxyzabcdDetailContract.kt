package io.example.fit.feature.abcdefghijklmnopqrstuvwxyzabcd.detail

import androidx.compose.runtime.Immutable
import io.example.fit.core.common.LoadState
import io.example.fit.core.mvi.UiEffect
import io.example.fit.core.mvi.UiEvent
import io.example.fit.core.mvi.UiState
import io.example.fit.data.abcdefghijklmnopqrstuvwxyzabcd.api.model.AbcdefghijklmnopqrstuvwxyzabcdItem

@Immutable
internal data class AbcdefghijklmnopqrstuvwxyzabcdDetailState(
    val loadState: LoadState = LoadState.Idle,
    val item: AbcdefghijklmnopqrstuvwxyzabcdItem? = null,
) : UiState

internal sealed interface AbcdefghijklmnopqrstuvwxyzabcdDetailEvent : UiEvent {
    data object Appeared : AbcdefghijklmnopqrstuvwxyzabcdDetailEvent
    data object Retry : AbcdefghijklmnopqrstuvwxyzabcdDetailEvent
    data object BackClicked : AbcdefghijklmnopqrstuvwxyzabcdDetailEvent
}

internal sealed interface AbcdefghijklmnopqrstuvwxyzabcdDetailEffect : UiEffect {
    data object NavigateBack : AbcdefghijklmnopqrstuvwxyzabcdDetailEffect
}
