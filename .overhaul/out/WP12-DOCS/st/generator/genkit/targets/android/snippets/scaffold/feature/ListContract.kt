package __PKG__.feature.__NAME__.list

import androidx.compose.runtime.Immutable
import __PKG__.core.common.LoadState
import __PKG__.core.mvi.UiEffect
import __PKG__.core.mvi.UiEvent
import __PKG__.core.mvi.UiState
import __PKG__.data.__NAME__.api.__P__Item

@Immutable
internal data class __P__ListState(
    val loadState: LoadState = LoadState.Idle,
    val items: List<__P__Item> = emptyList(),
) : UiState

internal sealed interface __P__ListEvent : UiEvent {
    data object Appeared : __P__ListEvent
    data object Refresh : __P__ListEvent
    data object Retry : __P__ListEvent
    data class ItemClicked(val id: String) : __P__ListEvent
}

internal sealed interface __P__ListEffect : UiEffect {
    data class OpenDetail(val id: String) : __P__ListEffect
}
