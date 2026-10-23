package io.example.fit.feature.abcdefghijklmnopqrstuvwxyzab.detail

import androidx.compose.runtime.Immutable
import io.example.fit.core.common.LoadState
import io.example.fit.core.mvi.UiEffect
import io.example.fit.core.mvi.UiEvent
import io.example.fit.core.mvi.UiState
import io.example.fit.data.abcdefghijklmnopqrstuvwxyzab.api.model.AbcdefghijklmnopqrstuvwxyzabItem

@Immutable
internal data class AbcdefghijklmnopqrstuvwxyzabDetailState(
    val loadState: LoadState = LoadState.Idle,
    val item: AbcdefghijklmnopqrstuvwxyzabItem? = null,
) : UiState

internal sealed interface AbcdefghijklmnopqrstuvwxyzabDetailEvent : UiEvent {
    data object Appeared : AbcdefghijklmnopqrstuvwxyzabDetailEvent
    data object Retry : AbcdefghijklmnopqrstuvwxyzabDetailEvent
    data object BackClicked : AbcdefghijklmnopqrstuvwxyzabDetailEvent
}

internal sealed interface AbcdefghijklmnopqrstuvwxyzabDetailEffect : UiEffect {
    data object NavigateBack : AbcdefghijklmnopqrstuvwxyzabDetailEffect
}
