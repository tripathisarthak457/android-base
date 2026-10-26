package com.example.finalafter.crnoui3.data.feed

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
}
