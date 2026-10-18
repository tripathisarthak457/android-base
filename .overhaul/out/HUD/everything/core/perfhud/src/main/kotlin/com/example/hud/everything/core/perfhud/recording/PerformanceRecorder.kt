package com.example.hud.everything.core.perfhud.recording

import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.BadParcelableException
import android.os.SystemClock
import com.example.hud.everything.core.common.coroutines.AppDispatchers
import com.example.hud.everything.core.common.coroutines.ApplicationScope
import com.example.hud.everything.core.common.environment.AppEnvironment
import com.example.hud.everything.core.devtools.log.DevToolsLog
import com.example.hud.everything.core.devtools.screen.CurrentScreen
import com.example.hud.everything.core.perfhud.checks.BuildChecks
import com.example.hud.everything.core.perfhud.checks.BuildFacts
import com.example.hud.everything.core.perfhud.checks.StrictModeWatch
import com.example.hud.everything.core.perfhud.frame.FrameFeed
import com.example.hud.everything.core.perfhud.frame.FrameIntake
import com.example.hud.everything.core.perfhud.frame.FrameLog
import com.example.hud.everything.core.perfhud.frame.RefreshRate
import com.example.hud.everything.core.perfhud.log.PerfLogcat
import com.example.hud.everything.core.perfhud.memory.MemoryLog
import com.example.hud.everything.core.perfhud.memory.MemorySampler
import com.example.hud.everything.core.perfhud.network.NetworkTimings
import com.example.hud.everything.core.perfhud.report.ExportedReport
import com.example.hud.everything.core.perfhud.score.OptimisationScore
import com.example.hud.everything.core.perfhud.score.ScoreInputs
import com.example.hud.everything.core.perfhud.startup.StartupSummary
import com.example.hud.everything.core.perfhud.startup.StartupTimes
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Starts and stops everything the overlay records, and holds what it has recorded.
 *
 * Recording is on by default in a debug build, where a developer wants to see it, and off in a
 * release-like dev build (devRelease, devBenchmarkRelease), which is what the perf gates measure.
 * The developer's own choice is kept across launches, and a launch with the extra
 * `perf.hud=on|off` overrides both for that process: that is how `perf.sh` measures what the
 * overlay itself costs.
 *
 * Nothing is allocated, listened to or sampled before the first recording starts.
 */
@Singleton
internal class PerformanceRecorder @Inject constructor(
    @ApplicationContext context: Context,
    private val feed: FrameFeed,
    private val settings: RecordingSettings,
    private val environment: AppEnvironment,
    private val currentScreen: CurrentScreen,
    private val requests: DevToolsLog,
    private val memorySampler: MemorySampler,
    private val startupTimes: StartupTimes,
    private val buildChecks: BuildChecks,
    private val strictMode: StrictModeWatch,
    private val logcat: PerfLogcat,
    private val scope: ApplicationScope,
    private val dispatchers: AppDispatchers,
) {
    private val refreshRate = RefreshRate(context.getSystemService(DisplayManager::class.java))
    private val _recording = MutableStateFlow(false)
    private var work: Job? = null
    private var launchOverride = false

    // Written on the main thread, read by snapshot() on a background one.
    @Volatile
    private var recordingSince = 0L

    @Volatile
    private var recordedBefore = 0L

    // Each taken once and reused: computing either twice at once only repeats the work.
    @Volatile
    private var buildFacts: BuildFacts? = null

    @Volatile
    private var startup: StartupSummary? = null

    @Volatile
    private var startupReadAt = 0L

    /** Whether frames are being recorded. Changes on the main thread. */
    val recording: StateFlow<Boolean> = _recording.asStateFlow()

    val frames: FrameLog by lazy { FrameLog() }

    val memory: MemoryLog by lazy { MemoryLog() }

    init {
        feed.onLaunch = ::applyLaunchOptions
        scope.launch(dispatchers.main) {
            val stored = settings.stored()
            if (!launchOverride) apply(stored ?: environment.isDebugBuild)
        }
    }

    /** Main thread. Kept across launches. */
    fun setRecording(on: Boolean) {
        apply(on)
        scope.launch { settings.save(on) }
    }

    /** Main thread. Forgets everything recorded and carries on recording if it was. */
    fun reset() {
        val wasRecording = _recording.value
        if (wasRecording) stop()
        frames.clear()
        memory.clear()
        strictMode.clear()
        recordedBefore = 0L
        if (wasRecording) start()
    }

    /** Everything recorded so far. Reads the start-up record and the build's profile state: main-safe. */
    suspend fun snapshot(): PerformanceSnapshot = withContext(dispatchers.default) {
        val frames = frames.summary()
        val memory = memory.summary()
        val startup = startupSummary()
        val build = buildFacts ?: buildChecks.read().also { buildFacts = it }
        val strictMode = strictMode.summary()
        PerformanceSnapshot(
            frames = frames,
            memory = memory,
            startup = startup,
            build = build,
            strictMode = strictMode,
            network = NetworkTimings.of(requests.exchanges.value),
            score = OptimisationScore.of(
                ScoreInputs(
                    frames = frames,
                    memory = memory,
                    startup = startup,
                    build = build,
                    strictMode = strictMode,
                ),
            ),
            recordedMillis = recordedMillis(),
            app = AppFacts(
                applicationId = environment.applicationId,
                versionName = environment.versionName,
                environment = environment.name,
                debugBuild = environment.isDebugBuild,
            ),
            device = DeviceFacts.current(refreshRate.supportedHz),
        )
    }

    fun logExport(report: ExportedReport) {
        logcat.note("exported json=${report.json.absolutePath} markdown=${report.markdown.absolutePath}")
    }

    private fun applyLaunchOptions(intent: Intent) {
        // Any app can start the launcher activity with any extras, so the value is read defensively
        // and only two exact values do anything; neither does more than switch this overlay.
        val value = try {
            intent.getStringExtra(LAUNCH_EXTRA)
        } catch (unreadable: BadParcelableException) {
            null
        }
        val on = when (value) {
            LAUNCH_ON -> true
            LAUNCH_OFF -> false
            else -> return
        }
        launchOverride = true
        apply(on)
    }

    private fun apply(on: Boolean) {
        if (on == _recording.value) return
        if (on) start() else stop()
    }

    private fun start() {
        refreshRate.start()
        strictMode.start()
        feed.start(FrameIntake(frames, refreshRate))
        recordingSince = SystemClock.elapsedRealtime()
        _recording.value = true
        if (environment.isDebugBuild) logcat.note(DEBUG_NOTE)
        work = scope.launch(dispatchers.main) {
            launch { currentScreen.name.filterNotNull().collect(feed::setRoute) }
            // Nothing is drawn in the background, and PSS there would only measure what the
            // system trimmed: the sampler stops until an activity is started again.
            launch(dispatchers.default) {
                feed.foreground.collectLatest { visible -> if (visible) sampleEverySecond() }
            }
        }
    }

    private fun stop() {
        work?.cancel()
        work = null
        feed.stop()
        strictMode.stop()
        refreshRate.stop()
        recordedBefore += SystemClock.elapsedRealtime() - recordingSince
        _recording.value = false
    }

    private suspend fun sampleEverySecond() {
        logcat.begin(frames)
        var second = 0L
        while (true) {
            memory.add(memorySampler.read(withPss = second % PSS_EVERY_SECONDS == 0L))
            logcat.onSecond(frames, memory, currentScreen.name.value)
            second++
            delay(SAMPLE_MILLIS)
        }
    }

    private fun startupSummary(): StartupSummary {
        val now = SystemClock.elapsedRealtime()
        val cached = startup
        if (cached != null && now - startupReadAt < STARTUP_REFRESH_MILLIS) return cached
        return startupTimes.read().also {
            startup = it
            startupReadAt = now
        }
    }

    private fun recordedMillis(): Long =
        recordedBefore + if (_recording.value) SystemClock.elapsedRealtime() - recordingSince else 0L

    private companion object {
        const val LAUNCH_EXTRA = "perf.hud"
        const val LAUNCH_ON = "on"
        const val LAUNCH_OFF = "off"
        const val SAMPLE_MILLIS = 1_000L

        /** PSS walks every mapping of the process, which takes milliseconds of a background core. */
        const val PSS_EVERY_SECONDS = 15L

        /** A start record only changes with a new start, which a panel left open sees within this. */
        const val STARTUP_REFRESH_MILLIS = 10_000L

        const val DEBUG_NOTE = "debug build: frame numbers are pessimistic (no R8, no baseline profile, " +
            "mostly interpreted); measure devRelease"
    }
}
