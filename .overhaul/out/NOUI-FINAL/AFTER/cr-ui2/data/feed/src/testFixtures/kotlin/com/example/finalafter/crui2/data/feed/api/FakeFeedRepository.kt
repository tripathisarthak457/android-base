package com.example.finalafter.crui2.data.feed.api

import androidx.paging.PagingData
import com.example.finalafter.crui2.data.feed.api.model.FeedPost
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * A [FeedRepository] whose feed is [posts], in one page. [fetches] counts how often a pager was
 * collected from the start, so a test can hold a screen to fetching the feed once, not once per
 * collector.
 */
class FakeFeedRepository(private val posts: List<FeedPost> = emptyList()) : FeedRepository {

    var fetches = 0
        private set

    override fun posts(): Flow<PagingData<FeedPost>> = flow {
        fetches++
        emit(PagingData.from(posts))
    }
}
