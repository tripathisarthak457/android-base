package com.example.finalafter.crnoui.ui

import com.example.finalafter.crnoui.ui.start.StartKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * The saved back stack holds the start key, so it has to be the same key in every process: one that
 * hashes by identity differs after process death and loses the restored stack on release builds.
 */
class AppDestinationsTest {

    @Test
    fun `the app starts on the start screen`() {
        assertEquals(StartKey, AppDestinations.start)
    }

    @Test
    fun `the start key hashes by value, the same in every process`() {
        val key = AppDestinations.start
        assertNotEquals(
            "${key::class.simpleName} hashes by identity: make it a data object or a data class",
            System.identityHashCode(key),
            key.hashCode(),
        )
    }
}
