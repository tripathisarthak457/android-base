package io.example.fit.feature.ab.detail

import androidx.compose.runtime.Immutable
import io.example.fit.core.common.LoadState
import io.example.fit.core.mvi.UiEffect
import io.example.fit.core.mvi.UiEvent
import io.example.fit.core.mvi.UiState
import io.example.fit.data.ab.api.model.AbItem

@Immutable
internal data class AbDetailState(
    val loadState: LoadState = LoadState.Idle,
    val item: AbItem? = null,
) : UiState

internal sealed interface AbDetailEvent : UiEvent {
    data object Appeared : AbDetailEvent
    data object Retry : AbDetailEvent
    data object BackClicked : AbDetailEvent
}

internal sealed interface AbDetailEffect : UiEffect {
    data object NavigateBack : AbDetailEffect
}
