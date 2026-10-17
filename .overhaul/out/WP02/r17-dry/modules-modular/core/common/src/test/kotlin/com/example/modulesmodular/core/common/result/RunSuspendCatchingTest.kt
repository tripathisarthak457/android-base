package com.example.modulesmodular.core.common.result

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RunSuspendCatchingTest {

    @Test
    fun `a value and a failure come back as a Result`() = runBlocking {
        assertEquals(Result.success(2), runSuspendCatching { 2 })

        val failure = runSuspendCatching { error("offline") }
        assertEquals("offline", failure.exceptionOrNull()?.message)
    }

    @Test
    fun `cancelling the coroutine stops it instead of becoming a failure`() = runBlocking {
        val started = CompletableDeferred<Unit>()
        var outcome: Result<Unit>? = null
        val work = async {
            outcome = runSuspendCatching {
                started.complete(Unit)
                awaitCancellation()
            }
        }

        started.await()
        work.cancel()
        yield()

        assertTrue(work.isCancelled)
        assertNull(outcome)
        assertTrue(runCatching { work.await() }.exceptionOrNull() is CancellationException)
    }
}
