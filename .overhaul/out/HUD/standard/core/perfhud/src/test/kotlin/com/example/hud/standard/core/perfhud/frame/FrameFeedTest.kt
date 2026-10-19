package com.example.hud.standard.core.perfhud.frame

import android.app.Activity
import android.app.Application
import android.os.Looper
import android.view.FrameMetrics
import androidx.metrics.performance.JankStats
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.shadows.FrameMetricsBuilder

/**
 * What the feed puts on the window decides whether the perf gates measure the app with the
 * overlay or without it, so it is checked on a real window: frames are reported through it as the
 * platform would, and a JankStats of the test's own reads the route the way every listener does.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class FrameFeedTest {

    private val application = ApplicationProvider.getApplicationContext<Application>()

    private val feed = frameFeed(application)

    private lateinit var activity: ActivityController<Activity>
    private val received = mutableListOf<Long>()
    private val routes = mutableListOf<String?>()
    private var vsync = Long.MAX_VALUE / 2

    private val listener = JankStats.OnFrameListener { frame -> received += frame.frameStartNanos }

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).create()
        // A plain activity creates its decor view only when asked, and JankStats needs one.
        activity.get().window.decorView
        activity.start().postCreate(null).resume().visible()
        JankStats.createAndTrack(activity.get().window) { frame ->
            routes += frame.states.firstOrNull { it.key == FrameIntake.ROUTE_STATE }?.value
        }
        settle()
    }

    @Test
    fun `nothing reaches the feed's listener before it starts`() {
        feed.setRoute("Home")

        drawFrame()

        assertEquals(0, received.size)
        assertEquals(listOf<String?>(null), routes)
    }

    @Test
    fun `a started feed hands the resumed activity's frames to its listener`() {
        feed.start(listener)
        settle()

        drawFrame()

        assertEquals(1, received.size)
    }

    @Test
    fun `a paused activity's frames do not reach the listener`() {
        feed.start(listener)
        settle()
        activity.pause()
        settle()

        drawFrame()

        assertEquals(0, received.size)
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
    }

    @Test
    fun `the route is on the window only while recording, and follows the screen`() {
        feed.setRoute("Home")
        feed.start(listener)
        settle()

        drawFrame()
        feed.setRoute("Detail")
        drawFrame()
        feed.stop()
        settle()
        drawFrame()

        assertEquals(listOf("Home", "Detail", null), routes)
    }

    @Test
    fun `a stop while the activity is paused still takes the route off its window`() {
        feed.setRoute("Home")
        feed.start(listener)
        settle()
        activity.pause()
        settle()

        feed.stop()
        activity.resume()
        settle()
        drawFrame()

        assertEquals(listOf<String?>(null), routes)
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
