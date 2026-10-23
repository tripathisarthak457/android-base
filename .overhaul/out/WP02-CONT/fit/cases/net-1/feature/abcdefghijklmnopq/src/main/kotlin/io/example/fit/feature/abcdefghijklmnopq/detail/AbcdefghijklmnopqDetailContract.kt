package io.example.fit.feature.abcdefghijklmnopq.detail

import androidx.compose.runtime.Immutable
import io.example.fit.core.common.LoadState
import io.example.fit.core.mvi.UiEffect
import io.example.fit.core.mvi.UiEvent
import io.example.fit.core.mvi.UiState
import io.example.fit.data.abcdefghijklmnopq.api.model.AbcdefghijklmnopqItem

@Immutable
internal data class AbcdefghijklmnopqDetailState(
    val loadState: LoadState = LoadState.Idle,
    val item: AbcdefghijklmnopqItem? = null,
) : UiState

internal sealed interface AbcdefghijklmnopqDetailEvent : UiEvent {
    data object Appeared : AbcdefghijklmnopqDetailEvent
    data object Retry : AbcdefghijklmnopqDetailEvent
    data object BackClicked : AbcdefghijklmnopqDetailEvent
}

internal sealed interface AbcdefghijklmnopqDetailEffect : UiEffect {
    data object NavigateBack : AbcdefghijklmnopqDetailEffect
}
