package io.example.fit.feature.abcdefgh.detail

import androidx.compose.runtime.Immutable
import io.example.fit.core.common.LoadState
import io.example.fit.core.mvi.UiEffect
import io.example.fit.core.mvi.UiEvent
import io.example.fit.core.mvi.UiState
import io.example.fit.data.abcdefgh.api.model.AbcdefghItem

@Immutable
internal data class AbcdefghDetailState(
    val loadState: LoadState = LoadState.Idle,
    val item: AbcdefghItem? = null,
) : UiState

internal sealed interface AbcdefghDetailEvent : UiEvent {
    data object Appeared : AbcdefghDetailEvent
    data object Retry : AbcdefghDetailEvent
    data object BackClicked : AbcdefghDetailEvent
}

internal sealed interface AbcdefghDetailEffect : UiEffect {
    data object NavigateBack : AbcdefghDetailEffect
}
