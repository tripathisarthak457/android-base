package io.example.fit.feature.abcdefghijklmn.detail

import androidx.compose.runtime.Immutable
import io.example.fit.core.common.LoadState
import io.example.fit.core.mvi.UiEffect
import io.example.fit.core.mvi.UiEvent
import io.example.fit.core.mvi.UiState
import io.example.fit.data.abcdefghijklmn.api.model.AbcdefghijklmnItem

@Immutable
internal data class AbcdefghijklmnDetailState(
    val loadState: LoadState = LoadState.Idle,
    val item: AbcdefghijklmnItem? = null,
) : UiState

internal sealed interface AbcdefghijklmnDetailEvent : UiEvent {
    data object Appeared : AbcdefghijklmnDetailEvent
    data object Retry : AbcdefghijklmnDetailEvent
    data object BackClicked : AbcdefghijklmnDetailEvent
}

internal sealed interface AbcdefghijklmnDetailEffect : UiEffect {
    data object NavigateBack : AbcdefghijklmnDetailEffect
}
