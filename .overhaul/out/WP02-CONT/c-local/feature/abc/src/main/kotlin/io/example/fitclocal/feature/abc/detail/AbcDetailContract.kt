package io.example.fitclocal.feature.abc.detail

import androidx.compose.runtime.Immutable
import io.example.fitclocal.core.common.LoadState
import io.example.fitclocal.core.mvi.UiEffect
import io.example.fitclocal.core.mvi.UiEvent
import io.example.fitclocal.core.mvi.UiState
import io.example.fitclocal.data.abc.api.model.AbcItem

@Immutable
internal data class AbcDetailState(
    val loadState: LoadState = LoadState.Idle,
    val item: AbcItem? = null,
) : UiState

internal sealed interface AbcDetailEvent : UiEvent {
    data object Appeared : AbcDetailEvent
    data object Retry : AbcDetailEvent
    data object BackClicked : AbcDetailEvent
}

internal sealed interface AbcDetailEffect : UiEffect {
    data object NavigateBack : AbcDetailEffect
}
