package com.example.wp04.dlplay.feature.feed

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.wp04.dlplay.core.mvi.MviViewModel
import com.example.wp04.dlplay.core.mvi.UiEffect
import com.example.wp04.dlplay.core.mvi.UiEvent
import com.example.wp04.dlplay.core.mvi.UiState
import com.example.wp04.dlplay.data.feed.api.FeedPost
import com.example.wp04.dlplay.data.feed.api.FeedRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@Immutable
internal data class FeedState(
    val expanded: Set<Int> = emptySet(),
) : UiState

internal sealed interface FeedEvent : UiEvent {
    data class ToggleExpanded(val postId: Int) : FeedEvent
}

internal sealed interface FeedEffect : UiEffect

/**
 * The pages themselves are not in [FeedState]: Paging owns them, and copying every loaded page into
 * a state object on each append would mean diffing the whole feed to add ten rows.
 */
@HiltViewModel
internal class FeedViewModel @Inject constructor(
    repository: FeedRepository,
) : MviViewModel<FeedState, FeedEvent, FeedEffect>(FeedState()) {

    val posts: Flow<PagingData<FeedPost>> = repository.posts().cachedIn(viewModelScope)

    override fun handleEvent(event: FeedEvent) {
        when (event) {
            is FeedEvent.ToggleExpanded -> setState {
                copy(expanded = if (event.postId in expanded) expanded - event.postId else expanded + event.postId)
            }
        }
    }
}
