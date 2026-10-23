package io.example.fit.feature.abcdefghi.detail

import androidx.compose.runtime.Immutable
import io.example.fit.core.common.LoadState
import io.example.fit.core.mvi.UiEffect
import io.example.fit.core.mvi.UiEvent
import io.example.fit.core.mvi.UiState
import io.example.fit.data.abcdefghi.api.model.AbcdefghiItem

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
