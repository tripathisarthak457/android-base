package com.example.finalafter.crui.feature.sample.detail

import androidx.compose.runtime.Immutable
import com.example.finalafter.crui.core.common.LoadState
import com.example.finalafter.crui.core.mvi.UiEffect
import com.example.finalafter.crui.core.mvi.UiEvent
import com.example.finalafter.crui.core.mvi.UiState
import com.example.finalafter.crui.data.sample.api.model.SampleItem

@Immutable
internal data class SampleDetailState(
    val loadState: LoadState = LoadState.Idle,
    val item: SampleItem? = null,
    val related: List<SampleItem> = emptyList(),
) : UiState

internal sealed interface SampleDetailEvent : UiEvent {
    data object Retry : SampleDetailEvent
    data object BackClicked : SampleDetailEvent
    data class RelatedClicked(val id: Int) : SampleDetailEvent
}

internal sealed interface SampleDetailEffect : UiEffect {
    data object NavigateBack : SampleDetailEffect
    data class OpenDetail(val id: Int) : SampleDetailEffect
}
