package com.example.cmp.data.feed.api

import androidx.paging.PagingData
import com.example.cmp.core.common.result.AppError
import com.example.cmp.data.feed.api.model.FeedPost
import kotlinx.coroutines.flow.Flow

/** The paged feed. */
interface FeedRepository {

    /**
     * An endless list, a page at a time. Each call builds a new pager, so call it once and keep the
     * flow (`cachedIn(viewModelScope)`); collect it with `collectAsLazyPagingItems()`.
     */
    fun posts(): Flow<PagingData<FeedPost>>
}

/** Carries the classified failure through Paging, which only knows about throwables. */
class FeedLoadException(val error: AppError) : Exception()
