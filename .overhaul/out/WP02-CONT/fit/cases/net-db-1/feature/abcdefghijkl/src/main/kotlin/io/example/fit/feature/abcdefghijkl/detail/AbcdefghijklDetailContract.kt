package io.example.fit.feature.abcdefghijkl.detail

import androidx.compose.runtime.Immutable
import io.example.fit.core.common.LoadState
import io.example.fit.core.mvi.UiEffect
import io.example.fit.core.mvi.UiEvent
import io.example.fit.core.mvi.UiState
import io.example.fit.data.abcdefghijkl.api.model.AbcdefghijklItem

@Immutable
internal data class AbcdefghijklDetailState(
    val loadState: LoadState = LoadState.Idle,
    val item: AbcdefghijklItem? = null,
) : UiState

internal sealed interface AbcdefghijklDetailEvent : UiEvent {
    data object Appeared : AbcdefghijklDetailEvent
    data object Retry : AbcdefghijklDetailEvent
    data object BackClicked : AbcdefghijklDetailEvent
}

internal sealed interface AbcdefghijklDetailEffect : UiEffect {
    data object NavigateBack : AbcdefghijklDetailEffect
}
