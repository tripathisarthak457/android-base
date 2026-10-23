package com.example.lean.feature.home.detail

import androidx.compose.runtime.Immutable
import com.example.lean.core.common.LoadState
import com.example.lean.core.mvi.UiEffect
import com.example.lean.core.mvi.UiEvent
import com.example.lean.core.mvi.UiState
import com.example.lean.data.home.api.HomeItem

@Immutable
internal data class HomeDetailState(
    val loadState: LoadState = LoadState.Idle,
    val item: HomeItem? = null,
) : UiState

internal sealed interface HomeDetailEvent : UiEvent {
    data object Appeared : HomeDetailEvent
    data object Retry : HomeDetailEvent
    data object BackClicked : HomeDetailEvent
}

internal sealed interface HomeDetailEffect : UiEffect {
    data object NavigateBack : HomeDetailEffect
}
