package com.example.modulescompact.feature.trips.detail

import androidx.compose.runtime.Immutable
import com.example.modulescompact.core.common.LoadState
import com.example.modulescompact.core.mvi.UiEffect
import com.example.modulescompact.core.mvi.UiEvent
import com.example.modulescompact.core.mvi.UiState
import com.example.modulescompact.data.trips.api.TripsItem

@Immutable
internal data class TripsDetailState(
    val loadState: LoadState = LoadState.Idle,
    val item: TripsItem? = null,
) : UiState

internal sealed interface TripsDetailEvent : UiEvent {
    data object Appeared : TripsDetailEvent
    data object Retry : TripsDetailEvent
    data object BackClicked : TripsDetailEvent
}

internal sealed interface TripsDetailEffect : UiEffect {
    data object NavigateBack : TripsDetailEffect
}
