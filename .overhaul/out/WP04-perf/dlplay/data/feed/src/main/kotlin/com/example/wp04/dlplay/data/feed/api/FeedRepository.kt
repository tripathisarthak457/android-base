package com.example.wp04.dlplay.data.feed.api

import androidx.paging.PagingData
import com.example.wp04.dlplay.core.common.result.AppError
import kotlinx.coroutines.flow.Flow

interface FeedRepository {

    /** An endless list, a page at a time. Collect it with `collectAsLazyPagingItems()`. */
    fun posts(): Flow<PagingData<FeedPost>>
}

/** Carries the classified failure through Paging, which only knows about throwables. */
class FeedLoadException(val error: AppError) : Exception()
