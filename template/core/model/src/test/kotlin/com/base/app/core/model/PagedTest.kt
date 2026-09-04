package com.base.app.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PagedTest {

    @Test
    fun `hasNext is false on the last partial page`() {
        val page = Paged(items = List(5) { it }, page = 3, pageSize = 20, totalItems = 45)
        assertFalse(page.hasNext)
    }

    @Test
    fun `hasNext is false when the last page is exactly full`() {
        val page = Paged(items = List(20) { it }, page = 2, pageSize = 20, totalItems = 40)
        assertFalse(page.hasNext)
    }

    @Test
    fun `hasNext is true while pages remain`() {
        val page = Paged(items = List(20) { it }, page = 1, pageSize = 20, totalItems = 45)
        assertTrue(page.hasNext)
    }

    @Test
    fun `appending the successor page concatenates items`() {
        val first = Paged(items = listOf(1, 2), page = 1, pageSize = 2, totalItems = 4)
        val second = Paged(items = listOf(3, 4), page = 2, pageSize = 2, totalItems = 4)

        val combined = first + second

        assertEquals(listOf(1, 2, 3, 4), combined.items)
        assertEquals(2, combined.page)
    }

    @Test
    fun `appending the same page twice is ignored`() {
        val first = Paged(items = listOf(1, 2), page = 1, pageSize = 2, totalItems = 4)

        val combined = first + first

        assertEquals(listOf(1, 2), combined.items)
    }
}
