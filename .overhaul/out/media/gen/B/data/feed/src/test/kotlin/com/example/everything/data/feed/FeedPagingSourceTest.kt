package com.example.everything.data.feed

import androidx.paging.PagingSource.LoadParams
import androidx.paging.PagingSource.LoadResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FeedPagingSourceTest {

    @Test
    fun `a full page has a next page`() {
        assertEquals(3, FeedPagingSource.nextPage(page = 2, loaded = 10, requested = 10))
    }

    @Test
    fun `a short page is the last`() {
        assertNull(FeedPagingSource.nextPage(page = 10, loaded = 4, requested = 10))
    }

    @Test
    fun `an empty page is the last`() {
        assertNull(FeedPagingSource.nextPage(page = 1, loaded = 0, requested = 10))
    }

    @Test
    fun `a first load and the next page do not overlap`() = runTest {
        val source = FeedPagingSource(FakeFeedClient())

        val first = source.load(LoadParams.Refresh(null, FeedPagingConfig.initialLoadSize, false)) as LoadResult.Page
        val second = source.load(LoadParams.Append(checkNotNull(first.nextKey), FeedPagingConfig.pageSize, false)) as LoadResult.Page

        assertEquals((1..30).toList(), first.data.map { it.id })
        assertEquals(2, first.nextKey)
        assertEquals((31..60).toList(), second.data.map { it.id })
    }
}
