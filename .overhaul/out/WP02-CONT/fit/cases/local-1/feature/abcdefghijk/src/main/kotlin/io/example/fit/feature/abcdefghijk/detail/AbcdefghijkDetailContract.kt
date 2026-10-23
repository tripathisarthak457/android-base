package io.example.fit.feature.abcdefghijk.detail

import androidx.compose.runtime.Immutable
import io.example.fit.core.common.LoadState
import io.example.fit.core.mvi.UiEffect
import io.example.fit.core.mvi.UiEvent
import io.example.fit.core.mvi.UiState
import io.example.fit.data.abcdefghijk.api.model.AbcdefghijkItem

@Immutable
internal data class AbcdefghijkDetailState(
    val loadState: LoadState = LoadState.Idle,
    val item: AbcdefghijkItem? = null,
) : UiState

internal sealed interface AbcdefghijkDetailEvent : UiEvent {
    data object Appeared : AbcdefghijkDetailEvent
    data object Retry : AbcdefghijkDetailEvent
    data object BackClicked : AbcdefghijkDetailEvent
}

internal sealed interface AbcdefghijkDetailEffect : UiEffect {
    data object NavigateBack : AbcdefghijkDetailEffect
}
