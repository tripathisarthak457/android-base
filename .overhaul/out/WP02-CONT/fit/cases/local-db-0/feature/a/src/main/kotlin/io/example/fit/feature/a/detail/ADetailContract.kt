package io.example.fit.feature.a.detail

import androidx.compose.runtime.Immutable
import io.example.fit.core.common.LoadState
import io.example.fit.core.mvi.UiEffect
import io.example.fit.core.mvi.UiEvent
import io.example.fit.core.mvi.UiState
import io.example.fit.data.a.api.model.AItem

@Immutable
internal data class ADetailState(
    val loadState: LoadState = LoadState.Idle,
    val item: AItem? = null,
) : UiState

internal sealed interface ADetailEvent : UiEvent {
    data object Appeared : ADetailEvent
    data object Retry : ADetailEvent
    data object BackClicked : ADetailEvent
}

internal sealed interface ADetailEffect : UiEffect {
    data object NavigateBack : ADetailEffect
}
