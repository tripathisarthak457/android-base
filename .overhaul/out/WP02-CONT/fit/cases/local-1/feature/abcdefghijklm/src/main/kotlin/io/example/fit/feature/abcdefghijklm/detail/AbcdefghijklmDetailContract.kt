package io.example.fit.feature.abcdefghijklm.detail

import androidx.compose.runtime.Immutable
import io.example.fit.core.common.LoadState
import io.example.fit.core.mvi.UiEffect
import io.example.fit.core.mvi.UiEvent
import io.example.fit.core.mvi.UiState
import io.example.fit.data.abcdefghijklm.api.model.AbcdefghijklmItem

@Immutable
internal data class AbcdefghijklmDetailState(
    val loadState: LoadState = LoadState.Idle,
    val item: AbcdefghijklmItem? = null,
) : UiState

internal sealed interface AbcdefghijklmDetailEvent : UiEvent {
    data object Appeared : AbcdefghijklmDetailEvent
    data object Retry : AbcdefghijklmDetailEvent
    data object BackClicked : AbcdefghijklmDetailEvent
}

internal sealed interface AbcdefghijklmDetailEffect : UiEffect {
    data object NavigateBack : AbcdefghijklmDetailEffect
}
