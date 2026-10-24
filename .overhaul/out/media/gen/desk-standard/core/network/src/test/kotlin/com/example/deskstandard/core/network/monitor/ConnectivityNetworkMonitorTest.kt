package com.example.deskstandard.core.network.monitor

import android.net.ConnectivityManager
import com.example.deskstandard.core.common.coroutines.AppDispatchers
import com.example.deskstandard.core.common.coroutines.ApplicationScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class ConnectivityNetworkMonitorTest {

    @Test
    fun `every request shares one connectivity registration`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        val dispatcher = StandardTestDispatcher(testScheduler)
        val monitor = ConnectivityNetworkMonitor(
            context = context,
            dispatchers = AppDispatchers(io = dispatcher, default = dispatcher, main = dispatcher),
            scope = ApplicationScope(backgroundScope),
        )

        List(REQUESTS) { async { monitor.isOnline.first() } }.awaitAll()
        runCurrent()

        assertEquals(1, shadowOf(connectivity).networkCallbacks.size)
    }

    private companion object {
        const val REQUESTS = 25
    }
}

// Robolectric refuses SDK 36 and above on a Java 17 toolchain.
private const val ROBOLECTRIC_SDK = 35
