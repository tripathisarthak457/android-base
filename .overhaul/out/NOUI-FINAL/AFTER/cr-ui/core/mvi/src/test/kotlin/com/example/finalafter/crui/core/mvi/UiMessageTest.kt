package com.example.finalafter.crui.core.mvi

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class UiMessageTest {

    @Test
    fun `the same words shown twice are two messages`() {
        val saved = UiText.Dynamic("Saved")

        assertNotEquals(UiMessage(saved), UiMessage(saved))
    }

    @Test
    fun `messages made on many threads at once are all distinct`() = runTest {
        val ids = withContext(Dispatchers.Default) {
            List(MESSAGES) { async { UiMessage(UiText.Dynamic("Saved")).id } }.awaitAll()
        }

        assertEquals(MESSAGES, ids.toSet().size)
    }

    private companion object {
        const val MESSAGES = 2_000
    }
}
