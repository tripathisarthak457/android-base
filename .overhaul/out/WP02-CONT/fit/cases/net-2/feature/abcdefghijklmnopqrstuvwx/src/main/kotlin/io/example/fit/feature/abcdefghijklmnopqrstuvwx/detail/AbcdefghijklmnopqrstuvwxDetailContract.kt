package io.example.fit.feature.abcdefghijklmnopqrstuvwx.detail

import androidx.compose.runtime.Immutable
import io.example.fit.core.common.LoadState
import io.example.fit.core.mvi.UiEffect
import io.example.fit.core.mvi.UiEvent
import io.example.fit.core.mvi.UiState
import io.example.fit.data.abcdefghijklmnopqrstuvwx.api.model.AbcdefghijklmnopqrstuvwxItem

@Immutable
internal data class AbcdefghijklmnopqrstuvwxDetailState(
    val loadState: LoadState = LoadState.Idle,
    val item: AbcdefghijklmnopqrstuvwxItem? = null,
) : UiState

internal sealed interface AbcdefghijklmnopqrstuvwxDetailEvent : UiEvent {
    data object Appeared : AbcdefghijklmnopqrstuvwxDetailEvent
    data object Retry : AbcdefghijklmnopqrstuvwxDetailEvent
    data object BackClicked : AbcdefghijklmnopqrstuvwxDetailEvent
}

internal sealed interface AbcdefghijklmnopqrstuvwxDetailEffect : UiEffect {
    data object NavigateBack : AbcdefghijklmnopqrstuvwxDetailEffect
}
