package com.base.app.feature.sample.list

import androidx.compose.runtime.Immutable
import com.base.app.core.common.mvi.LoadState
import com.base.app.core.common.mvi.UiEffect
import com.base.app.core.common.mvi.UiEvent
import com.base.app.core.common.mvi.UiState
import com.base.app.data.sample.SampleItem

/**
 * Everything the list screen renders.
 *
 * `@Immutable` tells the Compose compiler this can be compared by reference and skipped, which
 * only holds because every property is itself immutable — [items] is a read-only `List`, and the
 * ViewModel replaces it rather than mutating it. Annotating a state that holds a `MutableList` is
 * a promise the compiler believes and the code then breaks, producing a screen that silently
 * stops updating.
 *
 * [loadState] carries loading, empty and error together — see [LoadState] for why those are one
 * sealed value rather than a boolean and a nullable string.
 */
@Immutable
data class SampleListState(
    val loadState: LoadState = LoadState.Idle,
    val items: List<SampleItem> = emptyList(),
    val query: String = "",
    val isFromCache: Boolean = false,
) : UiState {

    /**
     * Filtering lives in the state, not in the composable.
     *
     * A composable that filters re-runs the filter on every recomposition, including ones caused
     * by something unrelated. Deriving it here means it is computed when the inputs change, and
     * the screen renders a list it does not have to think about.
     */
    val visibleItems: List<SampleItem>
        get() = if (query.isBlank()) {
            items
        } else {
            items.filter { it.title.contains(query, ignoreCase = true) }
        }
}

sealed interface SampleListEvent : UiEvent {
    data object Load : SampleListEvent
    data object Refresh : SampleListEvent
    data object Retry : SampleListEvent
    data class QueryChanged(val query: String) : SampleListEvent
    data class ItemClicked(val id: Int) : SampleListEvent
}

sealed interface SampleListEffect : UiEffect {
    data class OpenDetail(val id: Int) : SampleListEffect
}
