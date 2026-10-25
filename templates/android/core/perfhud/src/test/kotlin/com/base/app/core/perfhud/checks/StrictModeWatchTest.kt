package com.base.app.core.perfhud.checks

import android.os.Looper
import android.os.StrictMode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.base.app.core.testing.testDispatchers
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class StrictModeWatchTest {

    // Counts on the thread that reports, so a violation is counted by the time the looper is idle.
    private val watch = StrictModeWatch(testDispatchers(Dispatchers.Unconfined))
    private lateinit var appPolicy: StrictMode.ThreadPolicy

    @Before
    fun setUp() {
        appPolicy = StrictMode.ThreadPolicy.Builder().detectNetwork().penaltyLog().build()
        StrictMode.setThreadPolicy(appPolicy)
    }

    @After
    fun tearDown() {
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.LAX)
    }

    @Test
    fun `start adds the checks to the app's policy`() {
        watch.start()

        assertNotEquals(appPolicy.toString(), StrictMode.getThreadPolicy().toString())
    }

    @Test
    fun `stop puts back the policy the app had`() {
        watch.start()

        watch.stop()

        assertEquals(appPolicy.toString(), StrictMode.getThreadPolicy().toString())
    }

    @Test
    fun `a second start does not wrap the watched policy again`() {
        watch.start()
        watch.start()

        watch.stop()

        assertEquals(appPolicy.toString(), StrictMode.getThreadPolicy().toString())
    }

    @Test
    fun `a violation is counted by its kind, with where the app's code made it`() {
        watch.start()

        StrictMode.noteSlowCall("load")
        shadowOf(Looper.getMainLooper()).idle()

        val summary = watch.summary()
        assertEquals(mapOf("Custom" to 1), summary.byKind)
        val frame = summary.examples.single().frame.orEmpty()
        assertTrue(frame, frame.startsWith(StrictModeWatchTest::class.java.name))
    }

    @Test
    fun `nothing is counted once stopped`() {
        watch.start()
        watch.stop()

        StrictMode.noteSlowCall("load")
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(0, watch.summary().violations)
    }
}

// Robolectric refuses SDK 36 and above on a Java 17 toolchain, which is what this project builds with.
private const val ROBOLECTRIC_SDK = 35
