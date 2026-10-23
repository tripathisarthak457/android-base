package io.example.fitcnet.feature.abcdefghijklmnopqrs.detail

import androidx.compose.runtime.Immutable
import io.example.fitcnet.core.common.LoadState
import io.example.fitcnet.core.mvi.UiEffect
import io.example.fitcnet.core.mvi.UiEvent
import io.example.fitcnet.core.mvi.UiState
import io.example.fitcnet.data.abcdefghijklmnopqrs.api.model.AbcdefghijklmnopqrsItem

@Immutable
internal data class AbcdefghijklmnopqrsDetailState(
    val loadState: LoadState = LoadState.Idle,
    val item: AbcdefghijklmnopqrsItem? = null,
) : UiState

internal sealed interface AbcdefghijklmnopqrsDetailEvent : UiEvent {
    data object Appeared : AbcdefghijklmnopqrsDetailEvent
    data object Retry : AbcdefghijklmnopqrsDetailEvent
    data object BackClicked : AbcdefghijklmnopqrsDetailEvent
}

internal sealed interface AbcdefghijklmnopqrsDetailEffect : UiEffect {
    data object NavigateBack : AbcdefghijklmnopqrsDetailEffect
}
