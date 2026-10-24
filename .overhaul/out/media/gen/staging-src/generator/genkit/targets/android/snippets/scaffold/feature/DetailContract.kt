package __PKG__.feature.__NAME__.detail

import androidx.compose.runtime.Immutable
import __PKG__.core.common.LoadState
import __PKG__.core.mvi.UiEffect
import __PKG__.core.mvi.UiEvent
import __PKG__.core.mvi.UiState
import __PKG__.data.__NAME__.api.__P__Item

@Immutable
internal data class __P__DetailState(
    val loadState: LoadState = LoadState.Idle,
    val item: __P__Item? = null,
) : UiState

internal sealed interface __P__DetailEvent : UiEvent {
    data object Appeared : __P__DetailEvent
    data object Retry : __P__DetailEvent
    data object BackClicked : __P__DetailEvent
}

internal sealed interface __P__DetailEffect : UiEffect {
    data object NavigateBack : __P__DetailEffect
}
