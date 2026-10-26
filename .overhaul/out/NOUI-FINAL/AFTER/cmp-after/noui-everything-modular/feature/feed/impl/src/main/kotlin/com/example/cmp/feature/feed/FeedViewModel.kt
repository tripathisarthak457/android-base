package com.example.cmp.feature.feed

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.cmp.core.mvi.MviViewModel
import com.example.cmp.data.feed.api.FeedRepository
import com.example.cmp.data.feed.api.model.FeedPost
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * The pages themselves are not in [FeedState]: Paging owns them, and copying every loaded page into
 * a state object on each append would mean diffing the whole feed to add ten rows.
 */
@HiltViewModel
internal class FeedViewModel @Inject constructor(repository: FeedRepository) :
    MviViewModel<FeedState, FeedEvent, FeedEffect>(FeedState()) {

    val posts: Flow<PagingData<FeedPost>> = repository.posts().cachedIn(viewModelScope)

    override fun handleEvent(event: FeedEvent) {
        when (event) {
            is FeedEvent.ToggleExpanded -> setState {
                copy(
                    expanded = if (event.postId in
                        expanded
                    ) {
                        expanded - event.postId
                    } else {
                        expanded + event.postId
                    },
                )
            }
        }
    }
}
