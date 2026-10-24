package com.example.everything.data.feed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class FeedPagingConfigTest {

    @Test
    fun `the feed asks for its next page while twenty rows remain`() {
        assertEquals(30, FeedPagingConfig.pageSize)
        assertEquals(20, FeedPagingConfig.prefetchDistance)
        assertEquals(FeedPagingConfig.pageSize, FeedPagingConfig.initialLoadSize)
        assertFalse(FeedPagingConfig.enablePlaceholders)
    }
}
