package com.base.app.data.feed

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.base.app.core.common.result.AppResult
import com.base.app.core.network.client.NetworkClient
import com.base.app.core.network.client.get
import com.base.app.data.feed.api.FeedLoadException
import com.base.app.data.feed.api.model.FeedPost
import com.base.app.data.feed.remote.dto.FeedPostDto
import com.base.app.data.feed.remote.toDomain

internal class FeedPagingSource(private val networkClient: NetworkClient) :
    PagingSource<Int, FeedPost>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, FeedPost> {
        val page = params.key ?: FIRST_PAGE
        val result = networkClient.get<List<FeedPostDto>>(
            path = POSTS_PATH,
            query = mapOf("_page" to page, "_limit" to params.loadSize),
            requiresAuth = false,
        )
        return when (result) {
            is AppResult.Success -> LoadResult.Page(
                data = result.value.map { it.toDomain() },
                prevKey = if (page == FIRST_PAGE) null else page - 1,
                nextKey = nextPage(page, loaded = result.value.size, requested = params.loadSize),
            )

            is AppResult.Failure -> LoadResult.Error(FeedLoadException(result.error))
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

        // A public demo API, so the reference feed works on first run; a full URL never gets the token.
        private const val POSTS_PATH = "https://jsonplaceholder.typicode.com/posts"

        /** A short page is the last one. Asking again would only return an empty list. */
        fun nextPage(page: Int, loaded: Int, requested: Int): Int? =
            if (loaded < requested || loaded == 0) null else page + 1
    }
}
