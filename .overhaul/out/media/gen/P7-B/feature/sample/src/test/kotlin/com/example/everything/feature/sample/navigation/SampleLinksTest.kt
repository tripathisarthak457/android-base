package com.example.everything.feature.sample.navigation

import com.example.everything.core.navigation.DeepLink
import com.example.everything.feature.sample.api.SampleDetailKey
import com.example.everything.feature.sample.api.SampleListKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SampleLinksTest {

    private val route = SampleNavigation.sampleLinks()

    @Test
    fun `an item link opens that item`() {
        assertEquals(SampleDetailKey(12), route.keyFor(DeepLink(listOf("items", "12"), emptyMap())))
    }

    @Test
    fun `a link to the items, or to one that is not a number, opens the list`() {
        assertEquals(SampleListKey, route.keyFor(DeepLink(listOf("items"), emptyMap())))
        assertEquals(SampleListKey, route.keyFor(DeepLink(listOf("items", "latest"), emptyMap())))
    }

    @Test
    fun `another feature's link is left to that feature`() {
        assertNull(route.keyFor(DeepLink(listOf("orders", "3"), emptyMap())))
    }
}
