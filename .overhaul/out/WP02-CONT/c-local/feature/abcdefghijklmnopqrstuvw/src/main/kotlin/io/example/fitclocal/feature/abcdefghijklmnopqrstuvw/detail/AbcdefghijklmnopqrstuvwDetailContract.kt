package io.example.fitclocal.feature.abcdefghijklmnopqrstuvw.detail

import androidx.compose.runtime.Immutable
import io.example.fitclocal.core.common.LoadState
import io.example.fitclocal.core.mvi.UiEffect
import io.example.fitclocal.core.mvi.UiEvent
import io.example.fitclocal.core.mvi.UiState
import io.example.fitclocal.data.abcdefghijklmnopqrstuvw.api.model.AbcdefghijklmnopqrstuvwItem

@Immutable
internal data class AbcdefghijklmnopqrstuvwDetailState(
    val loadState: LoadState = LoadState.Idle,
    val item: AbcdefghijklmnopqrstuvwItem? = null,
) : UiState

internal sealed interface AbcdefghijklmnopqrstuvwDetailEvent : UiEvent {
    data object Appeared : AbcdefghijklmnopqrstuvwDetailEvent
    data object Retry : AbcdefghijklmnopqrstuvwDetailEvent
    data object BackClicked : AbcdefghijklmnopqrstuvwDetailEvent
}

internal sealed interface AbcdefghijklmnopqrstuvwDetailEffect : UiEffect {
    data object NavigateBack : AbcdefghijklmnopqrstuvwDetailEffect
}
