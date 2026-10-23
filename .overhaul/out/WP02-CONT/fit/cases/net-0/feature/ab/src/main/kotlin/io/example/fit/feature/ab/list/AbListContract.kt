package io.example.fit.feature.ab.list

import androidx.compose.runtime.Immutable
import io.example.fit.core.common.LoadState
import io.example.fit.core.mvi.UiEffect
import io.example.fit.core.mvi.UiEvent
import io.example.fit.core.mvi.UiState
import io.example.fit.data.ab.api.model.AbItem

@Immutable
internal data class AbListState(
    val loadState: LoadState = LoadState.Idle,
    val items: List<AbItem> = emptyList(),
) : UiState

internal sealed interface AbListEvent : UiEvent {
    data object Appeared : AbListEvent
    data object Refresh : AbListEvent
    data object Retry : AbListEvent
    data class ItemClicked(val id: String) : AbListEvent
}

internal sealed interface AbListEffect : UiEffect {
    data class OpenDetail(val id: String) : AbListEffect
}
