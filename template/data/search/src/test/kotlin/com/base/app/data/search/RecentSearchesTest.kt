package com.base.app.data.search

import org.junit.Assert.assertEquals
import org.junit.Test

class RecentSearchesTest {

    @Test
    fun `newest goes first and a repeat moves rather than duplicates`() {
        val list = RecentSearches.add(listOf("cats", "dogs"), "Dogs")
        assertEquals(listOf("Dogs", "cats"), list)
    }

    @Test
    fun `blank queries are not remembered`() {
        assertEquals(listOf("cats"), RecentSearches.add(listOf("cats"), "   "))
    }

    @Test
    fun `the list is capped`() {
        val full = (1..RecentSearches.LIMIT).map { "q$it" }
        assertEquals(RecentSearches.LIMIT, RecentSearches.add(full, "new").size)
    }

    @Test
    fun `encoding round-trips`() {
        val values = listOf("one", "two words", "three")
        assertEquals(values, RecentSearches.decode(RecentSearches.encode(values)))
        assertEquals(emptyList<String>(), RecentSearches.decode(null))
    }
}
