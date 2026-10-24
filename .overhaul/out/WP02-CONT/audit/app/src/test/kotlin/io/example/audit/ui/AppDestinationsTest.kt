package io.example.audit.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * The shell's saved state and its kept tabs are keyed on the tabs' root keys, never on how a tab is
 * drawn: an icon hashes by identity, so it differs between processes and loses state restored after
 * death on release builds.
 */
class AppDestinationsTest {

    @Test
    fun `every tab has its own root key`() {
        val keys = AppDestinations.tabs.map { it.key }

        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun `root keys hash by value, the same in every process`() {
        AppDestinations.tabs.map { it.key }.forEach { key ->
            assertNotEquals(
                "${key::class.simpleName} hashes by identity: make it a data object or a data class",
                System.identityHashCode(key),
                key.hashCode(),
            )
        }
    }
}
