package com.example.modulesmodular.feature.trips.list

import androidx.compose.runtime.Immutable
import com.example.modulesmodular.core.common.LoadState
import com.example.modulesmodular.core.mvi.UiEffect
import com.example.modulesmodular.core.mvi.UiEvent
import com.example.modulesmodular.core.mvi.UiState
import com.example.modulesmodular.data.trips.api.TripsItem

@Immutable
internal data class TripsListState(
    val loadState: LoadState = LoadState.Idle,
    val items: List<TripsItem> = emptyList(),
) : UiState

internal sealed interface TripsListEvent : UiEvent {
    data object Appeared : TripsListEvent
    data object Refresh : TripsListEvent
    data object Retry : TripsListEvent
    data class ItemClicked(val id: String) : TripsListEvent
}

internal sealed interface TripsListEffect : UiEffect {
    data class OpenDetail(val id: String) : TripsListEffect
}
