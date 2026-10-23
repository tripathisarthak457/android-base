package io.example.fit.feature.abcdefghijklmnopqrstuvwxyza.detail

import androidx.compose.runtime.Immutable
import io.example.fit.core.common.LoadState
import io.example.fit.core.mvi.UiEffect
import io.example.fit.core.mvi.UiEvent
import io.example.fit.core.mvi.UiState
import io.example.fit.data.abcdefghijklmnopqrstuvwxyza.api.model.AbcdefghijklmnopqrstuvwxyzaItem

@Immutable
internal data class AbcdefghijklmnopqrstuvwxyzaDetailState(
    val loadState: LoadState = LoadState.Idle,
    val item: AbcdefghijklmnopqrstuvwxyzaItem? = null,
) : UiState

internal sealed interface AbcdefghijklmnopqrstuvwxyzaDetailEvent : UiEvent {
    data object Appeared : AbcdefghijklmnopqrstuvwxyzaDetailEvent
    data object Retry : AbcdefghijklmnopqrstuvwxyzaDetailEvent
    data object BackClicked : AbcdefghijklmnopqrstuvwxyzaDetailEvent
}

internal sealed interface AbcdefghijklmnopqrstuvwxyzaDetailEffect : UiEffect {
    data object NavigateBack : AbcdefghijklmnopqrstuvwxyzaDetailEffect
}
