package com.example.cmp.data.feed.remote

import com.example.cmp.data.feed.remote.dto.FeedPostDto
import org.junit.Assert.assertEquals
import org.junit.Test

class FeedPostDtoMappersTest {

    @Test
    fun `titles are trimmed and capitalised`() {
        val post = FeedPostDto(id = 1, userId = 7, title = "  sunt aut facere ", body = " body ").toDomain()

        assertEquals("Sunt aut facere", post.title)
        assertEquals(7, post.authorId)
    }

    @Test
    fun `the excerpt runs the body's lines together and the body keeps them`() {
        val post = FeedPostDto(id = 1, body = "quia et suscipit\nsuscipit recusandae\n  consequuntur ").toDomain()

        assertEquals("quia et suscipit suscipit recusandae consequuntur", post.excerpt)
        assertEquals("quia et suscipit\nsuscipit recusandae\n  consequuntur", post.body)
    }
}
