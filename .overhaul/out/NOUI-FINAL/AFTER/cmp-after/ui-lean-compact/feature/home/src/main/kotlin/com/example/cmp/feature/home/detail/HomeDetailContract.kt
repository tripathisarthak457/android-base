package com.example.cmp.feature.home.detail

import androidx.compose.runtime.Immutable
import com.example.cmp.core.common.LoadState
import com.example.cmp.core.mvi.UiEffect
import com.example.cmp.core.mvi.UiEvent
import com.example.cmp.core.mvi.UiState
import com.example.cmp.data.home.api.HomeItem

@Immutable
internal data class HomeDetailState(
    val loadState: LoadState = LoadState.Idle,
    val item: HomeItem? = null,
) : UiState

internal sealed interface HomeDetailEvent : UiEvent {
    data object Retry : HomeDetailEvent
    data object BackClicked : HomeDetailEvent
}

internal sealed interface HomeDetailEffect : UiEffect {
    data object NavigateBack : HomeDetailEffect
}
