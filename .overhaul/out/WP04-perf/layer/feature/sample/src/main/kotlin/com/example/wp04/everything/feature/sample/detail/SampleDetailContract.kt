package com.example.wp04.everything.feature.sample.detail

import androidx.compose.runtime.Immutable
import com.example.wp04.everything.core.common.LoadState
import com.example.wp04.everything.core.mvi.UiEffect
import com.example.wp04.everything.core.mvi.UiEvent
import com.example.wp04.everything.core.mvi.UiState
import com.example.wp04.everything.data.sample.api.SampleItem

@Immutable
internal data class SampleDetailState(
    val loadState: LoadState = LoadState.Idle,
    val item: SampleItem? = null,
    val related: List<SampleItem> = emptyList(),
) : UiState

internal sealed interface SampleDetailEvent : UiEvent {
    data object Appeared : SampleDetailEvent
    data object Retry : SampleDetailEvent
    data object BackClicked : SampleDetailEvent
    data class RelatedClicked(val id: Int) : SampleDetailEvent
}

internal sealed interface SampleDetailEffect : UiEffect {
    data object NavigateBack : SampleDetailEffect
    data class OpenDetail(val id: Int) : SampleDetailEffect
}
