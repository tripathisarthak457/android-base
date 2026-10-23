package com.example.videoalone.feature.home.list

import androidx.compose.runtime.Immutable
import com.example.videoalone.core.common.LoadState
import com.example.videoalone.core.mvi.UiEffect
import com.example.videoalone.core.mvi.UiEvent
import com.example.videoalone.core.mvi.UiState
import com.example.videoalone.data.home.api.HomeItem

@Immutable
internal data class HomeListState(
    val loadState: LoadState = LoadState.Idle,
    val items: List<HomeItem> = emptyList(),
) : UiState

internal sealed interface HomeListEvent : UiEvent {
    data object Appeared : HomeListEvent
    data object Refresh : HomeListEvent
    data object Retry : HomeListEvent
    data class ItemClicked(val id: String) : HomeListEvent
}

internal sealed interface HomeListEffect : UiEffect {
    data class OpenDetail(val id: String) : HomeListEffect
}
