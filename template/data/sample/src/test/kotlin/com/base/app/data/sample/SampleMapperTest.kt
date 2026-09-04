package com.base.app.data.sample

import com.base.app.data.sample.remote.SampleDto
import com.base.app.data.sample.remote.toDomain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The mapper, which is where wire-format surprises become app-level bugs.
 */
class SampleMapperTest {

    @Test
    fun `a blank title becomes a placeholder rather than an empty row`() {
        val item = SampleDto(id = 1, title = "   ", body = "body").toDomain()

        assertEquals("Untitled", item.title)
    }

    @Test
    fun `whitespace around the wire values is trimmed`() {
        val item = SampleDto(id = 1, title = "  Hello  ", body = "\n body \n").toDomain()

        assertEquals("Hello", item.title)
        assertEquals("body", item.body)
    }

    @Test
    fun `a missing field decodes to its default instead of failing the whole list`() {
        val item = SampleDto(id = 7).toDomain()

        assertEquals(7, item.id)
        assertEquals("Untitled", item.title)
        assertEquals("", item.body)
    }

    @Test
    fun `preview collapses newlines and truncates long bodies`() {
        val body = "first line\nsecond line " + "x".repeat(200)
        val item = SampleDto(id = 1, title = "t", body = body).toDomain()

        assertTrue(item.preview.endsWith("…"))
        assertTrue('\n' !in item.preview)
        assertTrue(item.preview.length <= 91)
    }

    @Test
    fun `a short body is previewed whole, with no ellipsis`() {
        val item = SampleDto(id = 1, title = "t", body = "short").toDomain()

        assertEquals("short", item.preview)
    }
}
