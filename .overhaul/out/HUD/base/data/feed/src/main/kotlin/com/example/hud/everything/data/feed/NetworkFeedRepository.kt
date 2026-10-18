package com.example.hud.everything.data.feed

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.hud.everything.core.network.client.NetworkClient
import com.example.hud.everything.data.feed.api.FeedRepository
import com.example.hud.everything.data.feed.api.model.FeedPost
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pages through the same public API the reference feature reads, ten posts at a time, straight from
 * the network. Point [FeedPagingSource.load] at your endpoint; a cursor-based API uses a `String` key.
 */
@Singleton
internal class NetworkFeedRepository @Inject constructor(
    private val networkClient: NetworkClient,
) : FeedRepository {

    override fun posts(): Flow<PagingData<FeedPost>> = Pager(
        config = PagingConfig(
            pageSize = PAGE_SIZE,
            initialLoadSize = PAGE_SIZE,
            // Three rows before the end rather than a whole page: the next page is on screen
            // before the user reaches the footer, without fetching pages nobody scrolls to.
            prefetchDistance = 3,
            enablePlaceholders = false,
        ),
        pagingSourceFactory = { FeedPagingSource(networkClient) },
    ).flow

    private companion object {
        const val PAGE_SIZE = 10
    }
}
