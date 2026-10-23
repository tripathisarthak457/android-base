package io.example.fit.feature.abcdefghij.detail

import androidx.compose.runtime.Immutable
import io.example.fit.core.common.LoadState
import io.example.fit.core.mvi.UiEffect
import io.example.fit.core.mvi.UiEvent
import io.example.fit.core.mvi.UiState
import io.example.fit.data.abcdefghij.api.model.AbcdefghijItem

@Immutable
internal data class AbcdefghijDetailState(
    val loadState: LoadState = LoadState.Idle,
    val item: AbcdefghijItem? = null,
) : UiState

internal sealed interface AbcdefghijDetailEvent : UiEvent {
    data object Appeared : AbcdefghijDetailEvent
    data object Retry : AbcdefghijDetailEvent
    data object BackClicked : AbcdefghijDetailEvent
}

internal sealed interface AbcdefghijDetailEffect : UiEffect {
    data object NavigateBack : AbcdefghijDetailEffect
}
