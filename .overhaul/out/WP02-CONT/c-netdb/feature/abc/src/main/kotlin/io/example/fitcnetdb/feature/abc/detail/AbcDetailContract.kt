package io.example.fitcnetdb.feature.abc.detail

import androidx.compose.runtime.Immutable
import io.example.fitcnetdb.core.common.LoadState
import io.example.fitcnetdb.core.mvi.UiEffect
import io.example.fitcnetdb.core.mvi.UiEvent
import io.example.fitcnetdb.core.mvi.UiState
import io.example.fitcnetdb.data.abc.api.model.AbcItem

@Immutable
internal data class AbcDetailState(
    val loadState: LoadState = LoadState.Idle,
    val item: AbcItem? = null,
) : UiState

internal sealed interface AbcDetailEvent : UiEvent {
    data object Appeared : AbcDetailEvent
    data object Retry : AbcDetailEvent
    data object BackClicked : AbcDetailEvent
}

internal sealed interface AbcDetailEffect : UiEffect {
    data object NavigateBack : AbcDetailEffect
}
