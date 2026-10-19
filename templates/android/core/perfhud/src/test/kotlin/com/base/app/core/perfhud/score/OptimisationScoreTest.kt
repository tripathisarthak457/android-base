package com.base.app.core.perfhud.score

import com.base.app.core.perfhud.checks.BuildFacts
import com.base.app.core.perfhud.checks.ProfileStatus
import com.base.app.core.perfhud.checks.StrictModeSummary
import com.base.app.core.perfhud.frame.FrameStats
import com.base.app.core.perfhud.frame.FrameSummary
import com.base.app.core.perfhud.memory.MemorySummary
import com.base.app.core.perfhud.startup.StartRecord
import com.base.app.core.perfhud.startup.StartType
import com.base.app.core.perfhud.startup.StartupSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OptimisationScoreTest {

    @Test
    fun `an app that meets every target scores 100`() {
        val score = OptimisationScore.of(healthy)

        assertEquals(100, score.total)
        assertEquals(Grade.A, score.grade)
        assertTrue(score.losses.isEmpty())
    }

    @Test
    fun `each grade earns its share of the metric's weight`() {
        // 1.5 % janky is a C on a 25-point metric: 12.5 points lost out of 115.
        val score = OptimisationScore.of(healthy.copy(frames = frames(janky = 15)))

        val janky = score.metrics.single { it.metric == Metric.JankyFrames }
        assertEquals(Grade.C, janky.grade)
        assertEquals(12.5f, janky.lost, 0.001f)
        assertEquals(89, score.total)
        assertEquals(Metric.JankyFrames, score.losses.first().metric)
    }

    @Test
    fun `a debuggable build loses its build checks and says why`() {
        val score = OptimisationScore.of(
            healthy.copy(build = BuildFacts(debuggable = true, minified = false, profile = ProfileStatus.NotCompiled)),
        )

        val lost = score.losses.map { it.metric }.toSet()
        assertEquals(setOf(Metric.NotDebuggable, Metric.Minified, Metric.BaselineProfile), lost)
        assertEquals(87, score.total)
    }

    @Test
    fun `a profile waiting for compilation is half a pass`() {
        val score = OptimisationScore.of(
            healthy.copy(build = BuildFacts(debuggable = false, minified = true, profile = ProfileStatus.Enqueued)),
        )

        assertEquals(Grade.C, score.metrics.single { it.metric == Metric.BaselineProfile }.grade)
    }

    @Test
    fun `what was not measured is left out rather than counted as lost`() {
        val score = OptimisationScore.of(healthy.copy(startup = StartupSummary.Unsupported, memory = null))

        assertEquals(100, score.total)
        val unmeasured = score.metrics.filter { it.grade == null }.map { it.metric }.toSet()
        assertEquals(setOf(Metric.ColdStart, Metric.WarmStart, Metric.AppMemory, Metric.BlockingGc), unmeasured)
    }

    @Test
    fun `too few frames are not graded`() {
        val score = OptimisationScore.of(healthy.copy(frames = frames(count = Metric.MIN_FRAMES - 1, janky = 50)))

        assertNull(score.metrics.single { it.metric == Metric.JankyFrames }.grade)
    }

    @Test
    fun `nothing measured is no score`() {
        val score = OptimisationScore.of(ScoreInputs(null, null, null, null, null))

        assertNull(score.total)
        assertNull(score.grade)
    }

    @Test
    fun `passing build checks are no score before the frames are graded`() {
        // What the recorder hands over right after launch: the build read, StrictMode clean, a
        // start record, and too few frames to judge.
        val launched = healthy.copy(frames = frames(count = Metric.MIN_FRAMES - 1, janky = 0), memory = null)
        val noFrames = launched.copy(frames = null)

        for (inputs in listOf(launched, noFrames)) {
            val score = OptimisationScore.of(inputs)

            assertNull(score.total)
            assertNull(score.grade)
            assertEquals(Grade.A, score.metrics.single { it.metric == Metric.Minified }.grade)
        }
    }

    @Test
    fun `main-thread percentiles are judged against one refresh interval`() {
        // At 120 Hz the interval is 8.33 ms: a main-thread p90 of 6 ms is 0.72 of it, a B.
        val score = OptimisationScore.of(healthy.copy(frames = frames(uiP90 = 6f)))

        val p90 = score.metrics.single { it.metric == Metric.UiThreadP90 }
        assertEquals(0.72f, p90.value!!, 0.01f)
        assertEquals(Grade.B, p90.grade)
    }

    @Test
    fun `full frame times past one interval cost nothing when the main thread kept up`() {
        // A smooth scroll on a 120 Hz phone: frames run 9 and 17 ms from vsync to GPU done,
        // inside the deadline the pipeline gives them, with the main thread done in under 2 ms.
        val smooth = frames(janky = 6, p90 = 9f, p99 = 17f, uiP90 = 1.1f, uiP99 = 1.8f)

        val score = OptimisationScore.of(healthy.copy(frames = smooth))

        assertEquals(Grade.A, score.metrics.single { it.metric == Metric.UiThreadP90 }.grade)
        assertEquals(Grade.A, score.metrics.single { it.metric == Metric.UiThreadP99 }.grade)
        assertEquals(Grade.B, score.metrics.single { it.metric == Metric.JankyFrames }.grade)
    }

    @Test
    fun `a limit is inclusive`() {
        val limits = Limits(0.5f, 1f, 2f, 4f)

        assertEquals(Grade.A, limits.grade(0.5f))
        assertEquals(Grade.B, limits.grade(0.51f))
        assertEquals(Grade.D, limits.grade(4f))
        assertEquals(Grade.F, limits.grade(4.01f))
    }

    private fun frames(
        count: Int = 1000,
        janky: Int = 2,
        p90: Float = 4f,
        p99: Float = 7f,
        uiP90: Float = 3f,
        uiP99: Float = 5f,
    ) = FrameSummary(
        overall = FrameStats(
            frames = count,
            janky = janky,
            slow = 0,
            frozen = 0,
            longWork = 0,
            p50Millis = 3f,
            p90Millis = p90,
            p99Millis = p99,
            worstMillis = 12f,
        ),
        uiP50Millis = 2f,
        uiP90Millis = uiP90,
        uiP99Millis = uiP99,
        framesPerSecond = 119f,
        refreshHz = 120f,
        screens = emptyList(),
    )

    private val healthy = ScoreInputs(
        frames = frames(),
        memory = MemorySummary(
            javaHeapMb = 20f,
            peakJavaHeapMb = 24f,
            nativeHeapMb = 15f,
            peakNativeHeapMb = 16f,
            pssMb = 90f,
            peakPssMb = 95f,
            appPssMb = 40f,
            gcCount = 3,
            gcMillis = 12,
            blockingGcCount = 0,
            blockingGcMillis = 0,
        ),
        startup = StartupSummary(
            supported = true,
            cold = StartRecord(StartType.Cold, initialDisplayMillis = 350f, fullDisplayMillis = null),
            warm = StartRecord(StartType.Warm, initialDisplayMillis = 150f, fullDisplayMillis = null),
            hot = null,
        ),
        build = BuildFacts(debuggable = false, minified = true, profile = ProfileStatus.Compiled),
        strictMode = StrictModeSummary(supported = true, violations = 0, byKind = emptyMap(), examples = emptyList()),
    )
}
