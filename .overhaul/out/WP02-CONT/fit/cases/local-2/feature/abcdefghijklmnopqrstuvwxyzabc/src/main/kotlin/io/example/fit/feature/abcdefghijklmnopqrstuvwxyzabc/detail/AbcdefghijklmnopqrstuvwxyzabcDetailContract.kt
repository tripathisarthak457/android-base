package io.example.fit.feature.abcdefghijklmnopqrstuvwxyzabc.detail

import androidx.compose.runtime.Immutable
import io.example.fit.core.common.LoadState
import io.example.fit.core.mvi.UiEffect
import io.example.fit.core.mvi.UiEvent
import io.example.fit.core.mvi.UiState
import io.example.fit.data.abcdefghijklmnopqrstuvwxyzabc.api.model.AbcdefghijklmnopqrstuvwxyzabcItem

@Immutable
internal data class AbcdefghijklmnopqrstuvwxyzabcDetailState(
    val loadState: LoadState = LoadState.Idle,
    val item: AbcdefghijklmnopqrstuvwxyzabcItem? = null,
) : UiState

internal sealed interface AbcdefghijklmnopqrstuvwxyzabcDetailEvent : UiEvent {
    data object Appeared : AbcdefghijklmnopqrstuvwxyzabcDetailEvent
    data object Retry : AbcdefghijklmnopqrstuvwxyzabcDetailEvent
    data object BackClicked : AbcdefghijklmnopqrstuvwxyzabcDetailEvent
}

internal sealed interface AbcdefghijklmnopqrstuvwxyzabcDetailEffect : UiEffect {
    data object NavigateBack : AbcdefghijklmnopqrstuvwxyzabcDetailEffect
}
