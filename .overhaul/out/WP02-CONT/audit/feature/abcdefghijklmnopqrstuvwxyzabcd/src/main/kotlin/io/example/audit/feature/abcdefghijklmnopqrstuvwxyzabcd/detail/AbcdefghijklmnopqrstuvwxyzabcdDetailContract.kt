package io.example.audit.feature.abcdefghijklmnopqrstuvwxyzabcd.detail

import androidx.compose.runtime.Immutable
import io.example.audit.core.common.LoadState
import io.example.audit.core.mvi.UiEffect
import io.example.audit.core.mvi.UiEvent
import io.example.audit.core.mvi.UiState
import io.example.audit.data.abcdefghijklmnopqrstuvwxyzabcd.api.model.AbcdefghijklmnopqrstuvwxyzabcdItem

@Immutable
internal data class AbcdefghijklmnopqrstuvwxyzabcdDetailState(
    val loadState: LoadState = LoadState.Idle,
    val item: AbcdefghijklmnopqrstuvwxyzabcdItem? = null,
) : UiState

internal sealed interface AbcdefghijklmnopqrstuvwxyzabcdDetailEvent : UiEvent {
    data object Appeared : AbcdefghijklmnopqrstuvwxyzabcdDetailEvent
    data object Retry : AbcdefghijklmnopqrstuvwxyzabcdDetailEvent
    data object BackClicked : AbcdefghijklmnopqrstuvwxyzabcdDetailEvent
}

internal sealed interface AbcdefghijklmnopqrstuvwxyzabcdDetailEffect : UiEffect {
    data object NavigateBack : AbcdefghijklmnopqrstuvwxyzabcdDetailEffect
}
