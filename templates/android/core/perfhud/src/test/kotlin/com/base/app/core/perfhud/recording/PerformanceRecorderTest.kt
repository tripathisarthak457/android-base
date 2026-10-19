package com.base.app.core.perfhud.recording

import android.app.Activity
import android.app.Application
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.base.app.core.common.coroutines.AppDispatchers
import com.base.app.core.common.coroutines.ApplicationScope
import com.base.app.core.common.environment.AppEnvironment
import com.base.app.core.datastore.DataStoreFiles
import com.base.app.core.devtools.log.DevToolsLog
import com.base.app.core.devtools.screen.CurrentScreen
import com.base.app.core.perfhud.checks.BuildChecks
import com.base.app.core.perfhud.checks.StrictModeWatch
import com.base.app.core.perfhud.frame.FrameFeed
import com.base.app.core.perfhud.frame.frameFeed
import com.base.app.core.perfhud.log.PerfLogcat
import com.base.app.core.perfhud.memory.MemorySampler
import com.base.app.core.perfhud.startup.StartupTimes
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config

/**
 * Whether the overlay records decides whether the perf gates measure the app with it or without
 * it: release-like builds must start without it, and the launch extra `perf.sh` passes must win
 * over everything, including a stored choice read after the extra arrived.
 *
 * The stored choice is read on the test's dispatcher, so each test decides when that read lands.
 * Activities are only created, never started: the launch extra is read on creation, and a started
 * one would also start the memory sampler, which is not under test here.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class PerformanceRecorderTest {

    private val application = ApplicationProvider.getApplicationContext<Application>()

    // One file, as in the app: every launch in a test reads what the one before it kept.
    private lateinit var settings: RecordingSettings
    private val launched = mutableListOf<PerformanceRecorder>()

    // A recorder that records has replaced the main thread's StrictMode policy; it gives it back on stop.
    @After
    fun stopRecording() = launched.forEach { it.setRecording(false) }

    @Test
    fun `a debug build records from launch`() = runTest {
        val recorder = launch(debugBuild = true)

        runCurrent()

        assertTrue(recorder.recording.value)
    }

    @Test
    fun `a release-like build does not record from launch`() = runTest {
        val recorder = launch(debugBuild = false)

        runCurrent()

        assertFalse(recorder.recording.value)
    }

    @Test
    fun `a stored choice to record wins over a release-like build's default`() = runTest {
        val recorder = launch(debugBuild = false, stored = true)

        runCurrent()

        assertTrue(recorder.recording.value)
    }

    @Test
    fun `a stored choice not to record wins over a debug build's default`() = runTest {
        val recorder = launch(debugBuild = true, stored = false)

        runCurrent()

        assertFalse(recorder.recording.value)
    }

    @Test
    fun `the launch extra wins over the stored choice even when that is read after it`() = runTest {
        val recorder = launch(debugBuild = true, stored = true)

        createActivity(extra = "off")
        runCurrent()

        assertFalse(recorder.recording.value)
    }

    @Test
    fun `the launch extra turns recording on in a release-like build`() = runTest {
        val recorder = launch(debugBuild = false, stored = false)

        createActivity(extra = "on")
        runCurrent()

        assertTrue(recorder.recording.value)
    }

    @Test
    fun `any other value of the launch extra is ignored`() = runTest {
        val recorder = launch(debugBuild = false)

        createActivity(extra = "yes")
        runCurrent()

        assertFalse(recorder.recording.value)
    }

    @Test
    fun `switching recording is kept for the next launch`() = runTest {
        val recorder = launch(debugBuild = false)
        runCurrent()

        recorder.setRecording(true)
        runCurrent()

        assertTrue(recorder.recording.value)
        assertEquals(true, settings.stored())
    }

    @Test
    fun `reset forgets what was recorded and keeps recording`() = runTest {
        val recorder = launch(debugBuild = true)
        runCurrent()
        recorder.frames.add(
            startNanos = 0L,
            uiNanos = 1_000_000L,
            totalNanos = 5_000_000L,
            intervalNanos = 8_333_333L,
            missedDeadline = false,
            screen = "Home",
        )

        recorder.reset()

        assertTrue(recorder.recording.value)
        assertEquals(0, recorder.frames.summary().overall.frames)
    }

    @Test
    fun `reset leaves a stopped recorder stopped`() = runTest {
        val recorder = launch(debugBuild = false)
        runCurrent()

        recorder.reset()

        assertFalse(recorder.recording.value)
    }

    /**
     * A recorder as a fresh process builds it, with [stored] as the choice an earlier launch kept.
     * Its stored choice is read at the test's next `runCurrent`.
     */
    private suspend fun TestScope.launch(debugBuild: Boolean, stored: Boolean? = null): PerformanceRecorder {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val dispatchers = AppDispatchers(io = dispatcher, default = dispatcher, main = dispatcher)
        val scope = ApplicationScope(backgroundScope)
        if (!::settings.isInitialized) {
            settings = RecordingSettings(DataStoreFiles(application, scope, dispatchers))
        }
        if (stored != null) settings.save(stored)
        val feed: FrameFeed = frameFeed(application)
        val recorder = PerformanceRecorder(
            context = application,
            feed = feed,
            settings = settings,
            environment = AppEnvironment(
                name = "dev",
                versionName = "1.0.0",
                applicationId = "com.example.app.dev",
                apiBaseUrl = "https://example.com/",
                isDebugBuild = debugBuild,
            ),
            currentScreen = CurrentScreen(),
            requests = DevToolsLog(),
            memorySampler = MemorySampler(),
            startupTimes = StartupTimes(application),
            buildChecks = BuildChecks(application),
            strictMode = StrictModeWatch(),
            logcat = PerfLogcat(),
            scope = scope,
            dispatchers = dispatchers,
        )
        launched += recorder
        return recorder
    }

    private fun createActivity(extra: String) {
        val intent = Intent(application, Activity::class.java).putExtra(LAUNCH_EXTRA, extra)
        Robolectric.buildActivity(Activity::class.java, intent).create()
    }

    private companion object {
        const val LAUNCH_EXTRA = "perf.hud"
    }
}

// Robolectric refuses SDK 36 and above on a Java 17 toolchain, which is what this project builds with.
private const val ROBOLECTRIC_SDK = 35
