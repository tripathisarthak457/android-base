package io.example.fitcnetdb.feature.abcdefghijklmnopqrstuvwxyzabcd.detail

import androidx.compose.runtime.Immutable
import io.example.fitcnetdb.core.common.LoadState
import io.example.fitcnetdb.core.mvi.UiEffect
import io.example.fitcnetdb.core.mvi.UiEvent
import io.example.fitcnetdb.core.mvi.UiState
import io.example.fitcnetdb.data.abcdefghijklmnopqrstuvwxyzabcd.api.model.AbcdefghijklmnopqrstuvwxyzabcdItem

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
