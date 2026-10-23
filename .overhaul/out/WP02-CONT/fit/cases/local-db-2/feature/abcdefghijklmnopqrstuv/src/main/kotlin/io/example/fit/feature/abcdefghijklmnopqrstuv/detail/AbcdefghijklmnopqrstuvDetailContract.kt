package io.example.fit.feature.abcdefghijklmnopqrstuv.detail

import androidx.compose.runtime.Immutable
import io.example.fit.core.common.LoadState
import io.example.fit.core.mvi.UiEffect
import io.example.fit.core.mvi.UiEvent
import io.example.fit.core.mvi.UiState
import io.example.fit.data.abcdefghijklmnopqrstuv.api.model.AbcdefghijklmnopqrstuvItem

@Immutable
internal data class AbcdefghijklmnopqrstuvDetailState(
    val loadState: LoadState = LoadState.Idle,
    val item: AbcdefghijklmnopqrstuvItem? = null,
) : UiState

internal sealed interface AbcdefghijklmnopqrstuvDetailEvent : UiEvent {
    data object Appeared : AbcdefghijklmnopqrstuvDetailEvent
    data object Retry : AbcdefghijklmnopqrstuvDetailEvent
    data object BackClicked : AbcdefghijklmnopqrstuvDetailEvent
}

internal sealed interface AbcdefghijklmnopqrstuvDetailEffect : UiEffect {
    data object NavigateBack : AbcdefghijklmnopqrstuvDetailEffect
}
