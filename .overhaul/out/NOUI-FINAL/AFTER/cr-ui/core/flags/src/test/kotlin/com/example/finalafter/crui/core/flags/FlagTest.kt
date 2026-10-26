package com.example.finalafter.crui.core.flags

import org.junit.Assert.assertEquals
import org.junit.Test

class FlagTest {

    // Overrides and the vendor's values go by key: two flags sharing one would read each other's.
    @Test
    fun `every flag has a key of its own`() {
        val keys = Flag.ALL.map { it.key }

        assertEquals(keys.distinct(), keys)
    }
}
