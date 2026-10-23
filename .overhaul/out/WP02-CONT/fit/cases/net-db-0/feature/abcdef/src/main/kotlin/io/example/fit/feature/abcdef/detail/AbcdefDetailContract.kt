package io.example.fit.feature.abcdef.detail

import androidx.compose.runtime.Immutable
import io.example.fit.core.common.LoadState
import io.example.fit.core.mvi.UiEffect
import io.example.fit.core.mvi.UiEvent
import io.example.fit.core.mvi.UiState
import io.example.fit.data.abcdef.api.model.AbcdefItem

@Immutable
internal data class AbcdefDetailState(
    val loadState: LoadState = LoadState.Idle,
    val item: AbcdefItem? = null,
) : UiState

internal sealed interface AbcdefDetailEvent : UiEvent {
    data object Appeared : AbcdefDetailEvent
    data object Retry : AbcdefDetailEvent
    data object BackClicked : AbcdefDetailEvent
}

internal sealed interface AbcdefDetailEffect : UiEffect {
    data object NavigateBack : AbcdefDetailEffect
}
