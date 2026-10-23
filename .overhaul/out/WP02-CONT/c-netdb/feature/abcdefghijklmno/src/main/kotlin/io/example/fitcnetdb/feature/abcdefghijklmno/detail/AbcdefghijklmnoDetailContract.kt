package io.example.fitcnetdb.feature.abcdefghijklmno.detail

import androidx.compose.runtime.Immutable
import io.example.fitcnetdb.core.common.LoadState
import io.example.fitcnetdb.core.mvi.UiEffect
import io.example.fitcnetdb.core.mvi.UiEvent
import io.example.fitcnetdb.core.mvi.UiState
import io.example.fitcnetdb.data.abcdefghijklmno.api.model.AbcdefghijklmnoItem

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
