package com.base.app.data.feed

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.base.app.core.common.AppResult
import com.base.app.core.network.NetworkClient
import com.base.app.core.network.get
import com.base.app.data.feed.remote.FeedPostDto
import com.base.app.data.feed.remote.toDomain
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface FeedRepository {

    /** An endless list, a page at a time. Collect it with `collectAsLazyPagingItems()`. */
    fun posts(): Flow<PagingData<FeedPost>>
}

/**
 * Pages through the same public API the reference feature reads, ten posts at a time.
 *
 * Swap [FeedPagingSource.load] for your own endpoint: it is the only part that knows the API
 * pages by number. A cursor-based API keeps the same shape with a `String` key instead of an
 * `Int`.
 */
@Singleton
class DefaultFeedRepository @Inject constructor(
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

    internal companion object {
        const val PAGE_SIZE = 10
    }
}

internal class FeedPagingSource(
    private val networkClient: NetworkClient,
) : PagingSource<Int, FeedPost>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, FeedPost> {
        val page = params.key ?: FIRST_PAGE
        val result = networkClient.get<List<FeedPostDto>>(
            path = "posts",
            query = mapOf("_page" to page, "_limit" to params.loadSize),
            requiresAuth = false,
        )
        return when (result) {
            is AppResult.Success -> LoadResult.Page(
                data = result.data.map { it.toDomain() },
                prevKey = if (page == FIRST_PAGE) null else page - 1,
                nextKey = nextPage(page, loaded = result.data.size, requested = params.loadSize),
            )

            is AppResult.Failure -> LoadResult.Error(FeedLoadException(result))
        }
    }

    // A refresh starts again from the page the user was looking at, not from the top, so pulling
    // to refresh half way down the feed does not throw them back to the first post.
    override fun getRefreshKey(state: PagingState<Int, FeedPost>): Int? =
        state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.let { it.prevKey?.plus(1) ?: it.nextKey?.minus(1) }
        }

    internal companion object {
        const val FIRST_PAGE = 1

        /** A short page is the last one. Asking again would only return an empty list. */
        fun nextPage(page: Int, loaded: Int, requested: Int): Int? =
            if (loaded < requested || loaded == 0) null else page + 1
    }
}

/** Carries the classified failure through Paging, which only knows about throwables. */
class FeedLoadException(val failure: AppResult.Failure) : Exception(failure.message)
