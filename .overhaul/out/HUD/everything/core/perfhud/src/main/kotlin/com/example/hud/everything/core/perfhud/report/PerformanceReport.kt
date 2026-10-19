package com.example.hud.everything.core.perfhud.report

import android.content.res.Resources
import com.example.hud.everything.core.perfhud.R
import com.example.hud.everything.core.perfhud.checks.BuildFacts
import com.example.hud.everything.core.perfhud.checks.StrictModeSummary
import com.example.hud.everything.core.perfhud.frame.FrameSummary
import com.example.hud.everything.core.perfhud.memory.MemorySummary
import com.example.hud.everything.core.perfhud.network.NetworkTimings
import com.example.hud.everything.core.perfhud.recording.AppFacts
import com.example.hud.everything.core.perfhud.recording.DeviceFacts
import com.example.hud.everything.core.perfhud.recording.PerformanceSnapshot
import com.example.hud.everything.core.perfhud.score.doc
import com.example.hud.everything.core.perfhud.score.fix
import com.example.hud.everything.core.perfhud.startup.StartupSummary
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The exported report as JSON: everything the Performance tab shows, per screen, with the device
 * and build it was taken on and the score with each lost point's fix. [SCHEMA] changes when a
 * field is renamed or removed, so a script comparing two reports can tell.
 */
@Serializable
internal data class PerformanceReport(
    val schema: Int,
    val generatedAt: String,
    val app: AppFacts,
    val device: DeviceFacts,
    val build: BuildFacts,
    val recordedSeconds: Float,
    /** Caveats a reader needs before trusting a number, such as a debuggable build. */
    val notes: List<String>,
    val score: ScoreReport,
    val frames: FrameSummary,
    val memory: MemorySummary?,
    val startup: StartupSummary,
    val network: NetworkTimings,
    val strictMode: StrictModeSummary,
) {
    fun toJson(): String = JSON.encodeToString(serializer(), this)

    companion object {
        const val SCHEMA = 2

        private val JSON = Json {
            prettyPrint = true
            encodeDefaults = true
        }

        fun of(snapshot: PerformanceSnapshot, generatedAt: String, resources: Resources) = PerformanceReport(
            schema = SCHEMA,
            generatedAt = generatedAt,
            app = snapshot.app,
            device = snapshot.device,
            build = snapshot.build,
            recordedSeconds = snapshot.recordedMillis / MILLIS_PER_SECOND,
            notes = listOfNotNull(resources.getString(R.string.perfhud_debug_body).takeIf { snapshot.app.debugBuild }),
            score = ScoreReport(
                total = snapshot.score.total,
                grade = snapshot.score.grade?.name,
                metrics = snapshot.score.metrics.map { scored ->
                    MetricReport(
                        metric = scored.metric.name,
                        unit = scored.metric.unit.name,
                        value = scored.value,
                        target = scored.metric.limits?.a,
                        grade = scored.grade?.name,
                        points = scored.points,
                        maxPoints = scored.metric.weight,
                        fix = if (scored.lost > 0f) resources.getString(scored.metric.fix) else null,
                        doc = scored.metric.doc,
                    )
                },
            ),
            frames = snapshot.frames,
            memory = snapshot.memory,
            startup = snapshot.startup,
            network = snapshot.network,
            strictMode = snapshot.strictMode,
        )

        private const val MILLIS_PER_SECOND = 1000f
    }
}

/** Null [total] and [grade] until enough frames were recorded to grade them. */
@Serializable
internal data class ScoreReport(
    val total: Int?,
    val grade: String?,
    val metrics: List<MetricReport>,
)

/**
 * One metric. [target] is the value an A needs, in [unit]; null for a pass-or-fail check, which
 * reads 1 for a pass. [value] and [grade] are null when it was not measured.
 */
@Serializable
internal data class MetricReport(
    val metric: String,
    val unit: String,
    val value: Float?,
    val target: Float?,
    val grade: String?,
    val points: Float,
    val maxPoints: Int,
    val fix: String?,
    val doc: String,
)
