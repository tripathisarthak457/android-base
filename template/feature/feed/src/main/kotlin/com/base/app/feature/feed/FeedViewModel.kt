package com.base.app.feature.feed

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.base.app.core.common.mvi.MviViewModel
import com.base.app.core.common.mvi.UiEffect
import com.base.app.core.common.mvi.UiEvent
import com.base.app.core.common.mvi.UiState
import com.base.app.data.feed.FeedPost
import com.base.app.data.feed.FeedRepository
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
 * The pages themselves are not in [FeedState]: Paging owns them, and copying every loaded page
 * into a state object on each append would mean diffing the whole feed to add ten rows.
 * `cachedIn` keeps the loaded pages across a rotation and a tab switch, so coming back to the
 * feed shows where the user was rather than a spinner.
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
