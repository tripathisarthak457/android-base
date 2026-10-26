package com.example.finalafter.crui.core.notification.push

import com.example.finalafter.crui.core.common.coroutines.ApplicationScope
import com.example.finalafter.crui.core.datastore.FakePreferencesDataStore
import com.example.finalafter.crui.core.datastore.session.FakeAuthTokenStore
import java.io.IOException
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PushRegistrationTest {

    private val submitted = mutableListOf<String>()
    private var refusals = 0

    private val sink = PushTokenSink { installationId ->
        if (refusals > 0) {
            refusals--
            throw IOException("No network")
        }
        submitted += installationId
    }

    @Test
    fun `a new installation id is handed over once`() = runTest {
        val registration = registration()

        registration.onRegistered("fid-1")
        registration.onRegistered("fid-1")

        assertEquals(listOf("fid-1"), submitted)
    }

    @Test
    fun `a changed installation id is handed over again`() = runTest {
        val registration = registration()

        registration.onRegistered("fid-1")
        registration.onRegistered("fid-2")

        assertEquals(listOf("fid-1", "fid-2"), submitted)
    }

    // Push is a convenience: a failure is logged, and nothing records the id as delivered.
    @Test
    fun `an id the sink could not take is handed over again next time`() = runTest {
        val registration = registration()
        refusals = 1

        registration.onRegistered("fid-1")
        registration.onRegistered("fid-1")

        assertEquals(listOf("fid-1"), submitted)
    }

    private fun TestScope.registration() = PushRegistration(
        sink = sink,
        store = FakePreferencesDataStore(),
        tokenStore = FakeAuthTokenStore(),
        scope = ApplicationScope(backgroundScope),
    )
}
