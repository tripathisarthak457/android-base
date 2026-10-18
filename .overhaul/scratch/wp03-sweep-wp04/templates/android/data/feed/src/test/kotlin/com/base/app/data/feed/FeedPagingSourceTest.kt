package com.base.app.data.feed

import com.base.app.data.feed.remote.FeedPostDto
import com.base.app.data.feed.remote.toDomain
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
    fun `titles are trimmed and capitalised`() {
        val post = FeedPostDto(id = 1, userId = 7, title = "  sunt aut facere ", body = " body ").toDomain()
        assertEquals("Sunt aut facere", post.title)
        assertEquals("body", post.body)
        assertEquals(7, post.authorId)
    }
}
