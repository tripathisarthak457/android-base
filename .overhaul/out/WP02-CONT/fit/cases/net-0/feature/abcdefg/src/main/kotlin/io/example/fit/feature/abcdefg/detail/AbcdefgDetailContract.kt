package io.example.fit.feature.abcdefg.detail

import androidx.compose.runtime.Immutable
import io.example.fit.core.common.LoadState
import io.example.fit.core.mvi.UiEffect
import io.example.fit.core.mvi.UiEvent
import io.example.fit.core.mvi.UiState
import io.example.fit.data.abcdefg.api.model.AbcdefgItem

@Immutable
internal data class AbcdefgDetailState(
    val loadState: LoadState = LoadState.Idle,
    val item: AbcdefgItem? = null,
) : UiState

internal sealed interface AbcdefgDetailEvent : UiEvent {
    data object Appeared : AbcdefgDetailEvent
    data object Retry : AbcdefgDetailEvent
    data object BackClicked : AbcdefgDetailEvent
}

internal sealed interface AbcdefgDetailEffect : UiEffect {
    data object NavigateBack : AbcdefgDetailEffect
}
