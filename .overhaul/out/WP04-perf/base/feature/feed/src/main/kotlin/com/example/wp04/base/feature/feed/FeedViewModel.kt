package com.example.wp04.base.feature.feed

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.wp04.base.core.common.mvi.MviViewModel
import com.example.wp04.base.core.common.mvi.UiEffect
import com.example.wp04.base.core.common.mvi.UiEvent
import com.example.wp04.base.core.common.mvi.UiState
import com.example.wp04.base.data.feed.FeedPost
import com.example.wp04.base.data.feed.FeedRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@Immutable
data class FeedState(
    val expanded: Set<Int> = emptySet(),
) : UiState

sealed interface FeedEvent : UiEvent {
    data class ToggleExpanded(val postId: Int) : FeedEvent
}

sealed interface FeedEffect : UiEffect

/**
 * The pages themselves are not in [FeedState]: Paging owns them, and copying every loaded page into
 * a state object on each append would mean diffing the whole feed to add ten rows.
 */
@HiltViewModel
class FeedViewModel @Inject constructor(
    repository: FeedRepository,
) : MviViewModel<FeedState, FeedEvent, FeedEffect>(FeedState()) {
    val posts: Flow<PagingData<FeedPost>> = repository.posts().cachedIn(viewModelScope)

    override suspend fun handleEvent(event: FeedEvent) {
        when (event) {
            is FeedEvent.ToggleExpanded -> updateState {
                copy(expanded = if (event.postId in expanded) expanded - event.postId else expanded + event.postId)
            }
        }
    }
}
