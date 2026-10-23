package io.example.fit.feature.abcde.detail

import androidx.compose.runtime.Immutable
import io.example.fit.core.common.LoadState
import io.example.fit.core.mvi.UiEffect
import io.example.fit.core.mvi.UiEvent
import io.example.fit.core.mvi.UiState
import io.example.fit.data.abcde.api.model.AbcdeItem

@Immutable
internal data class AbcdeDetailState(
    val loadState: LoadState = LoadState.Idle,
    val item: AbcdeItem? = null,
) : UiState

internal sealed interface AbcdeDetailEvent : UiEvent {
    data object Appeared : AbcdeDetailEvent
    data object Retry : AbcdeDetailEvent
    data object BackClicked : AbcdeDetailEvent
}

internal sealed interface AbcdeDetailEffect : UiEffect {
    data object NavigateBack : AbcdeDetailEffect
}
