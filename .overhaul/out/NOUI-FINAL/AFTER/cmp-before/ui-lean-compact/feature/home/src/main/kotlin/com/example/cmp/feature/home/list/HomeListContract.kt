package com.example.cmp.feature.home.list

import androidx.compose.runtime.Immutable
import com.example.cmp.core.common.LoadState
import com.example.cmp.core.mvi.UiEffect
import com.example.cmp.core.mvi.UiEvent
import com.example.cmp.core.mvi.UiState
import com.example.cmp.data.home.api.HomeItem

@Immutable
internal data class HomeListState(
    val loadState: LoadState = LoadState.Idle,
    val items: List<HomeItem> = emptyList(),
) : UiState

internal sealed interface HomeListEvent : UiEvent {
    data object Refresh : HomeListEvent
    data object Retry : HomeListEvent
    data class ItemClicked(val id: String) : HomeListEvent
}

internal sealed interface HomeListEffect : UiEffect {
    data class OpenDetail(val id: String) : HomeListEffect
}
