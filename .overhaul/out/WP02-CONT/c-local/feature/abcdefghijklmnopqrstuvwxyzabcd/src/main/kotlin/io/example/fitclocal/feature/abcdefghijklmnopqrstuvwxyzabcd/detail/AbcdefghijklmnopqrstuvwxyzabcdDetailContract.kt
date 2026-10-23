package io.example.fitclocal.feature.abcdefghijklmnopqrstuvwxyzabcd.detail

import androidx.compose.runtime.Immutable
import io.example.fitclocal.core.common.LoadState
import io.example.fitclocal.core.mvi.UiEffect
import io.example.fitclocal.core.mvi.UiEvent
import io.example.fitclocal.core.mvi.UiState
import io.example.fitclocal.data.abcdefghijklmnopqrstuvwxyzabcd.api.model.AbcdefghijklmnopqrstuvwxyzabcdItem

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
