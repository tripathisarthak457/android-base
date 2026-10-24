package com.example.everything.core.perfhud.frame

import android.app.Activity
import android.app.Application
import android.os.Looper
import android.view.FrameMetrics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.metrics.performance.JankStats
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.everything.core.analytics.jank.FrameJankRecorder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.shadows.FrameMetricsBuilder

/**
 * What the feed does to the window decides whether the perf gates measure the app with the overlay
 * or without it, so it is checked on a real window: frames are reported through it as the platform
 * would, and a JankStats of the test's own reads the window's states the way every listener does.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class FrameFeedTest {

    private val application = ApplicationProvider.getApplicationContext<Application>()

    // The analytics recorder watches the window whether or not anything records, as in the app.
    private val jankRecorder: FrameJankRecorder = jankRecorder()
    private val analyticsLifecycle = object : LifecycleOwner {
        override val lifecycle = LifecycleRegistry.createUnsafe(this)
    }
    private val feed = frameFeed(application, jankRecorder)

    private lateinit var activity: ActivityController<Activity>
    private val received = mutableListOf<Long>()
    private val statesPerFrame = mutableListOf<Int>()
    private var vsync = Long.MAX_VALUE / 2

    private val listener = JankStats.OnFrameListener { frame -> received += frame.frameStartNanos }

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).create()
        // A plain activity creates its decor view only when asked, and JankStats needs one.
        activity.get().window.decorView
        activity.start().postCreate(null).resume().visible()
        jankRecorder.track(activity.get().window, analyticsLifecycle.lifecycle)
        analyticsLifecycle.lifecycle.currentState = Lifecycle.State.RESUMED
        JankStats.createAndTrack(activity.get().window) { frame -> statesPerFrame += frame.states.size }
        settle()
    }

    @Test
    fun `nothing reaches the feed's listener before it starts`() {
        drawFrame()

        assertEquals(0, received.size)
        assertNull(jankRecorder.frameListener)
    }

    @Test
    fun `a started feed hands the resumed activity's frames to its listener`() {
        feed.start(listener)
        settle()

        drawFrame()

        assertEquals(1, received.size)
        assertNotNull(jankRecorder.frameListener)
    }

    @Test
    fun `a paused activity's frames do not reach the listener`() {
        feed.start(listener)
        settle()
        activity.pause()
        settle()

        drawFrame()

        assertEquals(0, received.size)
        assertNull(jankRecorder.frameListener)
    }

    @Test
    fun `frames reach the listener again when the activity resumes`() {
        feed.start(listener)
        settle()
        activity.pause()
        settle()
        activity.resume()
        settle()

        drawFrame()

        assertEquals(1, received.size)
    }

    @Test
    fun `a stopped feed hands over nothing`() {
        feed.start(listener)
        settle()
        feed.stop()
        settle()

        drawFrame()

        assertEquals(0, received.size)
        assertNull(jankRecorder.frameListener)
    }

    @Test
    fun `a recording puts no state on the window`() {
        feed.start(listener)
        settle()

        repeat(3) { drawFrame() }

        assertEquals(3, received.size)
        // The analytics recorder keys each frame by these states: none keeps its frames under
        // "idle", with no key to build, whether or not the overlay records.
        assertEquals(listOf(0, 0, 0), statesPerFrame)
    }

    @Test
    fun `a stop with two activities resumed leaves neither window feeding the listener`() {
        feed.start(listener)
        settle()
        val second = Robolectric.buildActivity(Activity::class.java).create()
        second.get().window.decorView
        second.start().postCreate(null).resume().visible()
        settle()

        feed.stop()
        settle()
        drawFrame()

        assertEquals(0, received.size)
    }

    private fun drawFrame() {
        // Far past any clock the states were stamped with, so a frame sees every state put
        // before it and none removed before it; each one later than the last, as frames are.
        vsync += FRAME_NANOS
        val metrics = FrameMetricsBuilder().setMetric(FrameMetrics.INTENDED_VSYNC_TIMESTAMP, vsync).build()
        shadowOf(activity.get().window).reportOnFrameMetricsAvailable(metrics)
    }

    /** JankStats attaches to and leaves the window in a message posted to the main thread. */
    private fun settle() = shadowOf(Looper.getMainLooper()).idle()

    private companion object {
        const val FRAME_NANOS = 8_333_333L
    }
}

// Robolectric refuses SDK 36 and above on a Java 17 toolchain, which is what this project builds with.
private const val ROBOLECTRIC_SDK = 35
