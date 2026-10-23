package io.example.fitcnet.feature.abcdefghijklmno.detail

import androidx.compose.runtime.Immutable
import io.example.fitcnet.core.common.LoadState
import io.example.fitcnet.core.mvi.UiEffect
import io.example.fitcnet.core.mvi.UiEvent
import io.example.fitcnet.core.mvi.UiState
import io.example.fitcnet.data.abcdefghijklmno.api.model.AbcdefghijklmnoItem

@Immutable
internal data class AbcdefghijklmnoDetailState(
    val loadState: LoadState = LoadState.Idle,
    val item: AbcdefghijklmnoItem? = null,
) : UiState

internal sealed interface AbcdefghijklmnoDetailEvent : UiEvent {
    data object Appeared : AbcdefghijklmnoDetailEvent
    data object Retry : AbcdefghijklmnoDetailEvent
    data object BackClicked : AbcdefghijklmnoDetailEvent
}

internal sealed interface AbcdefghijklmnoDetailEffect : UiEffect {
    data object NavigateBack : AbcdefghijklmnoDetailEffect
}
