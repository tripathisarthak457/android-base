package com.example.everything.data.feed

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.everything.core.network.client.NetworkClient
import com.example.everything.data.feed.api.FeedRepository
import com.example.everything.data.feed.api.model.FeedPost
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * Pages through the same public API the reference feature reads, thirty posts at a time, straight from
 * the network. Point [FeedPagingSource.load] at your endpoint; a cursor-based API uses a `String` key.
 */
@Singleton
internal class NetworkFeedRepository @Inject constructor(
    private val networkClient: NetworkClient,
) : FeedRepository {

    override fun posts(): Flow<PagingData<FeedPost>> = Pager(
        config = FeedPagingConfig,
        pagingSourceFactory = { FeedPagingSource(networkClient) },
    ).flow
}

/**
 * Thirty posts a page, the next one asked for while twenty rows remain: about five screens of feed
 * cards, more than a hard fling coasts (about 6,000 px on a 1080 x 2400 phone) plus what is scrolled
 * while a page is on its way, so a fling never stops at the loading footer. The first load is one
 * page because [FeedPagingSource] numbers its pages: a bigger first load would overlap page 2.
 */
internal val FeedPagingConfig = PagingConfig(
    pageSize = 30,
    prefetchDistance = 20,
    initialLoadSize = 30,
    enablePlaceholders = false,
)
