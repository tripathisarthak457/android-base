package com.example.deskstandard.core.perfhud.score

import com.example.deskstandard.core.perfhud.checks.BuildFacts
import com.example.deskstandard.core.perfhud.checks.ProfileStatus
import com.example.deskstandard.core.perfhud.checks.StrictModeSummary
import com.example.deskstandard.core.perfhud.frame.FrameLog
import com.example.deskstandard.core.perfhud.frame.FrameStats
import com.example.deskstandard.core.perfhud.frame.FrameSummary
import com.example.deskstandard.core.perfhud.memory.MemorySummary
import com.example.deskstandard.core.perfhud.startup.StartRecord
import com.example.deskstandard.core.perfhud.startup.StartType
import com.example.deskstandard.core.perfhud.startup.StartupSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OptimisationScoreTest {

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

    private var clock = 0L

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
    fun `a single StrictMode violation is a B, not a step straight to C`() {
        fun gradeOf(violations: Int) = OptimisationScore.of(
            healthy.copy(
                strictMode = StrictModeSummary(
                    supported = true,
                    violations = violations,
                    byKind = emptyMap(),
                    examples = emptyList(),
                ),
            ),
        ).metrics.single { it.metric == Metric.StrictModeClean }.grade

        assertEquals(listOf(Grade.A, Grade.B, Grade.C), (0..2).map(::gradeOf))
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
    fun `a frame is judged against the interval it was drawn at, whichever rate came last`() {
        // A 120 Hz scroll whose slowest frames keep the main thread busy for 12 ms, 1.44 of the
        // interval, and one lone frame at 60 Hz, as an adaptive display draws after idle. Pooled
        // times over the newest frame's interval would grade the p99 an A or a B by that order.
        val lastAt60 = FrameLog().apply {
            drawScroll()
            drawFrame(uiMillis = 2f, intervalNanos = INTERVAL_60_HZ)
        }.summary()
        val lastAt120 = FrameLog().apply {
            drawFrame(uiMillis = 2f, intervalNanos = INTERVAL_60_HZ)
            drawScroll()
        }.summary()

        val grades = listOf(lastAt60, lastAt120).map { frames ->
            OptimisationScore.of(healthy.copy(frames = frames)).metrics.single { it.metric == Metric.UiThreadP99 }
        }

        assertEquals(1.44f, grades[0].value!!, 0.03f)
        assertEquals(Grade.B, grades[0].grade)
        assertEquals(grades[0], grades[1])
        assertEquals(120f, lastAt60.refreshHz, 0.1f)
    }

    @Test
    fun `a limit is inclusive`() {
        val limits = Limits(0.5f, 1f, 2f, 4f)

        assertEquals(Grade.A, limits.grade(0.5f))
        assertEquals(Grade.B, limits.grade(0.51f))
        assertEquals(Grade.D, limits.grade(4f))
        assertEquals(Grade.F, limits.grade(4.01f))
    }

    private fun FrameLog.drawScroll() {
        repeat(970) { drawFrame(uiMillis = 2f, intervalNanos = INTERVAL_120_HZ) }
        repeat(30) { drawFrame(uiMillis = 12f, intervalNanos = INTERVAL_120_HZ) }
    }

    private fun FrameLog.drawFrame(uiMillis: Float, intervalNanos: Long) {
        clock += intervalNanos
        add(
            startNanos = clock,
            uiNanos = (uiMillis * MILLI).toLong(),
            totalNanos = (uiMillis * 2 * MILLI).toLong(),
            intervalNanos = intervalNanos,
            missedDeadline = false,
            screen = "Home",
        )
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
        uiP90BudgetShare = uiP90 / INTERVAL_120_HZ_MILLIS,
        uiP99BudgetShare = uiP99 / INTERVAL_120_HZ_MILLIS,
        framesPerSecond = 119f,
        refreshHz = 120f,
        screens = emptyList(),
    )

    private companion object {
        const val MILLI = 1_000_000f
        const val INTERVAL_120_HZ = 8_333_333L
        const val INTERVAL_60_HZ = 16_666_667L
        const val INTERVAL_120_HZ_MILLIS = 8.333333f
    }
}
