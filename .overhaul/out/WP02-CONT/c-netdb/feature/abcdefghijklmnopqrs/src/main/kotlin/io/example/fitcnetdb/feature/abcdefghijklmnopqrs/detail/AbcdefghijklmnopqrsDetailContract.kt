package io.example.fitcnetdb.feature.abcdefghijklmnopqrs.detail

import androidx.compose.runtime.Immutable
import io.example.fitcnetdb.core.common.LoadState
import io.example.fitcnetdb.core.mvi.UiEffect
import io.example.fitcnetdb.core.mvi.UiEvent
import io.example.fitcnetdb.core.mvi.UiState
import io.example.fitcnetdb.data.abcdefghijklmnopqrs.api.model.AbcdefghijklmnopqrsItem

@Immutable
internal data class AbcdefghijklmnopqrsDetailState(
    val loadState: LoadState = LoadState.Idle,
    val item: AbcdefghijklmnopqrsItem? = null,
) : UiState

internal sealed interface AbcdefghijklmnopqrsDetailEvent : UiEvent {
    data object Appeared : AbcdefghijklmnopqrsDetailEvent
    data object Retry : AbcdefghijklmnopqrsDetailEvent
    data object BackClicked : AbcdefghijklmnopqrsDetailEvent
}

internal sealed interface AbcdefghijklmnopqrsDetailEffect : UiEffect {
    data object NavigateBack : AbcdefghijklmnopqrsDetailEffect
}
