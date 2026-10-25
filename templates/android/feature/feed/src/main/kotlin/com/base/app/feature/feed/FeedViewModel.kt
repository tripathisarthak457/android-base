package com.base.app.feature.feed

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.base.app.core.mvi.MviViewModel
import com.base.app.data.feed.api.FeedRepository
import com.base.app.data.feed.api.model.FeedPost
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

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
