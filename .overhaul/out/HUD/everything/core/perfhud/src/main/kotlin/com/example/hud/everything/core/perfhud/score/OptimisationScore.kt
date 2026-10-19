package com.example.hud.everything.core.perfhud.score

import com.example.hud.everything.core.perfhud.checks.BuildFacts
import com.example.hud.everything.core.perfhud.checks.ProfileStatus
import com.example.hud.everything.core.perfhud.checks.StrictModeSummary
import com.example.hud.everything.core.perfhud.frame.FrameSummary
import com.example.hud.everything.core.perfhud.memory.MemorySummary
import com.example.hud.everything.core.perfhud.startup.StartupSummary
import kotlin.math.roundToInt

/**
 * A score from 0 to 100 for how well the app runs, out of a grade per [Metric] against the
 * template's own targets: the device gates in `tools/perf/gates.toml`, the smoothness target of
 * about no janky frames, and the build facts every fast release shares.
 *
 * A metric that was not measured, such as start-up on a release without start records, is left
 * out of the total rather than counted as lost. The frames are what the score is about, so there
 * is no score until they are graded: the build checks alone would rate an app nobody has used.
 */
internal data class OptimisationScore(
    /** Null until [Metric.MIN_FRAMES] frames have been recorded. */
    val total: Int?,
    /** Null with [total]. */
    val grade: Grade?,
    val metrics: List<MetricScore>,
) {
    /** The measured metrics that lost points, most points first. */
    val losses: List<MetricScore>
        get() = metrics.filter { it.lost > 0f }.sortedByDescending { it.lost }

    companion object {
        fun of(inputs: ScoreInputs): OptimisationScore {
            val metrics = Metric.entries.map { it.score(inputs) }
            if (metrics.single { it.metric == Metric.JankyFrames }.grade == null) {
                return OptimisationScore(total = null, grade = null, metrics = metrics)
            }
            val graded = metrics.filter { it.grade != null }
            val possible = graded.sumOf { it.metric.weight }
            val earned = graded.sumOf { it.points.toDouble() }
            val total = (earned * MAX_SCORE / possible).roundToInt()
            return OptimisationScore(total = total, grade = Grade.forScore(total), metrics = metrics)
        }

        private const val MAX_SCORE = 100
    }
}

/** What the score is taken from; null for what has not been measured. */
internal data class ScoreInputs(
    val frames: FrameSummary?,
    val memory: MemorySummary?,
    val startup: StartupSummary?,
    val build: BuildFacts?,
    val strictMode: StrictModeSummary?,
)

/**
 * One metric's result. [value] is in the metric's [MetricUnit]; [grade] is null when there was
 * nothing to grade, and the metric then counts for nothing either way.
 */
internal data class MetricScore(
    val metric: Metric,
    val value: Float?,
    val grade: Grade?,
) {
    val points: Float get() = if (grade == null) 0f else metric.weight * grade.share
    val lost: Float get() = if (grade == null) 0f else metric.weight - points
}

internal enum class Grade(val share: Float) {
    A(1f),
    B(0.75f),
    C(0.5f),
    D(0.25f),
    F(0f),
    ;

    companion object {
        fun forScore(score: Int): Grade = when {
            score >= 90 -> A
            score >= 80 -> B
            score >= 65 -> C
            score >= 50 -> D
            else -> F
        }
    }
}

internal enum class MetricUnit { Percent, BudgetShare, PerThousandFrames, Millis, Megabytes, Count, Check }

/**
 * The most a value may be for each grade from A to D; past [d] it is an F. A is the target.
 */
internal class Limits(val a: Float, val b: Float, val c: Float, val d: Float) {
    fun grade(value: Float): Grade = when {
        value <= a -> Grade.A
        value <= b -> Grade.B
        value <= c -> Grade.C
        value <= d -> Grade.D
        else -> Grade.F
    }
}

/**
 * Each metric, its weight in the score and how it is graded. The limits come from
 * `tools/perf/gates.toml` where it has one (janky frames 0.5 % on a scroll, cold start 380 ms, the
 * app's own PSS 48 MB), from Android vitals for slow and frozen frames, and otherwise from the
 * nearest gate: a warm start in half the cold-start limit.
 *
 * The percentiles are of each frame's main-thread time against its own refresh interval, the time
 * the main thread has before the next frame starts, so a display that moves between 60 and
 * 120 Hz holds each frame to the rate it was drawn at; `gates.toml` measures frame times on the UI
 * thread too. A frame's full duration, vsync to GPU done, is no measure of this: the render
 * pipeline gives a frame more than one interval, so on-time frames routinely run past one.
 * Whether a frame made its deadline is [JankyFrames].
 *
 * A check is 1 when it passes and 0 when it fails; a baseline profile waiting for compilation is
 * 0.5, a C.
 */
internal enum class Metric(val weight: Int, val unit: MetricUnit, val limits: Limits?) {
    JankyFrames(25, MetricUnit.Percent, Limits(0.5f, 1f, 2f, 4f)) {
        override fun valueOf(inputs: ScoreInputs) = inputs.ofFrames { it.overall.jankyPercent }
    },
    UiThreadP90(10, MetricUnit.BudgetShare, Limits(0.6f, 0.8f, 1f, 1.5f)) {
        override fun valueOf(inputs: ScoreInputs) = inputs.ofFrames { it.uiP90BudgetShare }
    },
    UiThreadP99(10, MetricUnit.BudgetShare, Limits(1f, 1.5f, 2f, 3f)) {
        override fun valueOf(inputs: ScoreInputs) = inputs.ofFrames { it.uiP99BudgetShare }
    },
    SlowFrames(5, MetricUnit.Percent, Limits(0.5f, 1f, 5f, 25f)) {
        override fun valueOf(inputs: ScoreInputs) = inputs.ofFrames { it.overall.slowPercent }
    },
    FrozenFrames(10, MetricUnit.Percent, Limits(0f, 0.01f, 0.05f, 0.1f)) {
        override fun valueOf(inputs: ScoreInputs) = inputs.ofFrames { it.overall.frozenPercent }
    },
    LongMainThreadWork(5, MetricUnit.PerThousandFrames, Limits(0f, 1f, 3f, 10f)) {
        override fun valueOf(inputs: ScoreInputs) =
            inputs.ofFrames { it.overall.longWork * PER_THOUSAND / it.overall.frames }
    },
    ColdStart(10, MetricUnit.Millis, Limits(380f, 500f, 700f, 1000f)) {
        override fun valueOf(inputs: ScoreInputs) = inputs.startup?.cold?.initialDisplayMillis
    },
    WarmStart(5, MetricUnit.Millis, Limits(190f, 250f, 400f, 700f)) {
        override fun valueOf(inputs: ScoreInputs) = inputs.startup?.warm?.initialDisplayMillis
    },
    AppMemory(10, MetricUnit.Megabytes, Limits(48f, 60f, 80f, 120f)) {
        override fun valueOf(inputs: ScoreInputs) = inputs.memory?.appPssMb
    },
    BlockingGc(5, MetricUnit.Count, Limits(0f, 1f, 3f, 10f)) {
        override fun valueOf(inputs: ScoreInputs) = inputs.memory?.blockingGcCount?.toFloat()
    },
    StrictModeClean(5, MetricUnit.Count, Limits(0f, 0f, 2f, 5f)) {
        override fun valueOf(inputs: ScoreInputs) =
            inputs.strictMode?.takeIf { it.supported }?.violations?.toFloat()
    },
    Minified(5, MetricUnit.Check, null) {
        override fun valueOf(inputs: ScoreInputs) = inputs.build?.minified?.let(::check)
    },
    BaselineProfile(5, MetricUnit.Check, null) {
        override fun valueOf(inputs: ScoreInputs) = inputs.build?.profile?.let { profile ->
            when (profile) {
                ProfileStatus.Compiled -> PASS
                ProfileStatus.Enqueued -> HALF
                ProfileStatus.NotCompiled, ProfileStatus.NotInApk, ProfileStatus.NotInProject -> FAIL
            }
        }
    },
    NotDebuggable(5, MetricUnit.Check, null) {
        override fun valueOf(inputs: ScoreInputs) = inputs.build?.debuggable?.let { check(!it) }
    },
    ;

    /** The measured value in [unit], or null when there is nothing to grade. */
    protected abstract fun valueOf(inputs: ScoreInputs): Float?

    fun score(inputs: ScoreInputs): MetricScore {
        val value = valueOf(inputs) ?: return MetricScore(this, value = null, grade = null)
        val grade = limits?.grade(value) ?: when (value) {
            PASS -> Grade.A
            HALF -> Grade.C
            else -> Grade.F
        }
        return MetricScore(this, value, grade)
    }

    companion object {
        /**
         * Fewer frames than this say more about chance than about the app: one slow frame in a
         * dozen would read as 8 % jank.
         */
        const val MIN_FRAMES = 300

        private const val PASS = 1f
        private const val HALF = 0.5f
        private const val FAIL = 0f
        private const val PER_THOUSAND = 1000f

        private fun check(passed: Boolean): Float = if (passed) PASS else FAIL
    }
}

private inline fun ScoreInputs.ofFrames(value: (FrameSummary) -> Float): Float? {
    val frames = frames?.takeIf { it.overall.frames >= Metric.MIN_FRAMES && it.refreshHz > 0f } ?: return null
    return value(frames)
}
