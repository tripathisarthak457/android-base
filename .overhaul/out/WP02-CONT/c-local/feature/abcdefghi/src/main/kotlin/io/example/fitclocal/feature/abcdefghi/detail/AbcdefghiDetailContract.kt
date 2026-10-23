package io.example.fitclocal.feature.abcdefghi.detail

import androidx.compose.runtime.Immutable
import io.example.fitclocal.core.common.LoadState
import io.example.fitclocal.core.mvi.UiEffect
import io.example.fitclocal.core.mvi.UiEvent
import io.example.fitclocal.core.mvi.UiState
import io.example.fitclocal.data.abcdefghi.api.model.AbcdefghiItem

@Immutable
internal data class AbcdefghiDetailState(
    val loadState: LoadState = LoadState.Idle,
    val item: AbcdefghiItem? = null,
) : UiState

internal sealed interface AbcdefghiDetailEvent : UiEvent {
    data object Appeared : AbcdefghiDetailEvent
    data object Retry : AbcdefghiDetailEvent
    data object BackClicked : AbcdefghiDetailEvent
}

internal sealed interface AbcdefghiDetailEffect : UiEffect {
    data object NavigateBack : AbcdefghiDetailEffect
}
