package io.example.fit.feature.abcd.detail

import androidx.compose.runtime.Immutable
import io.example.fit.core.common.LoadState
import io.example.fit.core.mvi.UiEffect
import io.example.fit.core.mvi.UiEvent
import io.example.fit.core.mvi.UiState
import io.example.fit.data.abcd.api.model.AbcdItem

@Immutable
internal data class AbcdDetailState(
    val loadState: LoadState = LoadState.Idle,
    val item: AbcdItem? = null,
) : UiState

internal sealed interface AbcdDetailEvent : UiEvent {
    data object Appeared : AbcdDetailEvent
    data object Retry : AbcdDetailEvent
    data object BackClicked : AbcdDetailEvent
}

internal sealed interface AbcdDetailEffect : UiEffect {
    data object NavigateBack : AbcdDetailEffect
}
