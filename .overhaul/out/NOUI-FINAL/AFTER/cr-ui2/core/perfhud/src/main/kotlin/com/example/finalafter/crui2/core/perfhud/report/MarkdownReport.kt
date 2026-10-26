package com.example.finalafter.crui2.core.perfhud.report

import android.content.res.Resources
import androidx.annotation.StringRes
import com.example.finalafter.crui2.core.perfhud.R
import com.example.finalafter.crui2.core.perfhud.checks.ProfileStatus
import com.example.finalafter.crui2.core.perfhud.checks.strictModeExample
import com.example.finalafter.crui2.core.perfhud.frame.FrameStats
import com.example.finalafter.crui2.core.perfhud.frame.ScreenFrames
import com.example.finalafter.crui2.core.perfhud.recording.PerformanceSnapshot
import com.example.finalafter.crui2.core.perfhud.score.Metric
import com.example.finalafter.crui2.core.perfhud.score.decimal
import com.example.finalafter.crui2.core.perfhud.score.doc
import com.example.finalafter.crui2.core.perfhud.score.fix
import com.example.finalafter.crui2.core.perfhud.score.formatTarget
import com.example.finalafter.crui2.core.perfhud.score.formatValue
import com.example.finalafter.crui2.core.perfhud.score.minutesAndSeconds
import com.example.finalafter.crui2.core.perfhud.score.title
import com.example.finalafter.crui2.core.perfhud.startup.StartRecord

/**
 * The exported report as Markdown, for a person: the score and its lost points first, because
 * that is what to act on, then each measurement in a table. Copy comes from the module's
 * resources, so the report and the panel say the same thing in the same words.
 */
internal class MarkdownReport(private val resources: Resources) {

    fun write(snapshot: PerformanceSnapshot, generatedAt: String): String = buildString {
        header(snapshot, generatedAt)
        score(snapshot)
        frames(snapshot)
        memory(snapshot)
        startup(snapshot)
        network(snapshot)
        strictMode(snapshot)
        build(snapshot)
    }

    private fun StringBuilder.header(snapshot: PerformanceSnapshot, generatedAt: String) {
        val app = snapshot.app
        val device = snapshot.device
        heading(1, string(R.string.perfhud_report_title, app.applicationId, app.versionName, app.environment))
        paragraph(
            string(
                R.string.perfhud_report_generated,
                generatedAt,
                device.manufacturer,
                device.model,
                device.release,
                device.sdk,
                device.refreshRatesHz.joinToString("/") { decimal(it, 0) },
            ) + " " + string(R.string.perfhud_report_recorded, minutesAndSeconds(snapshot.recordedMillis)),
        )
        if (app.debugBuild) {
            append("> **").append(string(R.string.perfhud_debug_title)).append("**. ")
                .append(string(R.string.perfhud_debug_body)).append("\n\n")
        }
    }

    private fun StringBuilder.score(snapshot: PerformanceSnapshot) {
        val score = snapshot.score
        heading(2, string(R.string.perfhud_score))
        val total = score.total
        val grade = score.grade
        if (total == null || grade == null) {
            paragraph(resources.getQuantityString(R.plurals.perfhud_score_none, Metric.MIN_FRAMES, Metric.MIN_FRAMES))
            return
        }
        paragraph("**" + string(R.string.perfhud_score_value, total, grade.name) + "**")
        row(
            string(R.string.perfhud_column_metric),
            string(R.string.perfhud_column_value),
            string(R.string.perfhud_column_target),
            string(R.string.perfhud_column_grade),
            string(R.string.perfhud_column_points),
        )
        tableRule(5)
        score.metrics.forEach { scored ->
            row(
                string(scored.metric.title),
                scored.value?.let { resources.formatValue(scored.metric, it) } ?: string(R.string.perfhud_not_measured),
                resources.formatTarget(scored.metric),
                scored.grade?.name.orEmpty(),
                string(R.string.perfhud_points_of, decimal(scored.points, 1), scored.metric.weight),
            )
        }
        append('\n')
        heading(3, string(R.string.perfhud_losses))
        val losses = score.losses
        if (losses.isEmpty()) paragraph(string(R.string.perfhud_no_losses))
        losses.forEach { lost ->
            append("- **").append(string(lost.metric.title)).append("** ")
                .append(string(R.string.perfhud_lost_points, decimal(lost.lost, 1), lost.metric.weight))
                .append(". ").append(string(lost.metric.fix)).append(' ')
                .append(string(R.string.perfhud_doc, "`" + lost.metric.doc + "`")).append('\n')
        }
        append('\n')
    }

    private fun StringBuilder.frames(snapshot: PerformanceSnapshot) {
        val frames = snapshot.frames
        val overall = frames.overall
        heading(2, string(R.string.perfhud_frames))
        table(
            string(R.string.perfhud_frames) to overall.frames.toString(),
            string(R.string.perfhud_fps) to decimal(frames.framesPerSecond, 1),
            string(R.string.perfhud_display) to string(R.string.perfhud_hz, Math.round(frames.refreshHz)),
            string(R.string.perfhud_janky) to countAndPercent(overall.janky, overall.jankyPercent),
            string(R.string.perfhud_slow) to countAndPercent(overall.slow, overall.slowPercent),
            string(R.string.perfhud_frozen) to overall.frozen.toString(),
            string(R.string.perfhud_long_work) to overall.longWork.toString(),
            string(R.string.perfhud_percentiles) to percentiles(overall),
            string(R.string.perfhud_ui_percentiles) to string(
                R.string.perfhud_three_values,
                millis(frames.uiP50Millis),
                millis(frames.uiP90Millis),
                millis(frames.uiP99Millis),
            ),
            string(R.string.perfhud_worst) to millis(overall.worstMillis),
        )
        heading(3, string(R.string.perfhud_by_screen))
        row(
            string(R.string.perfhud_screen),
            string(R.string.perfhud_frames),
            string(R.string.perfhud_janky),
            string(R.string.perfhud_slow),
            string(R.string.perfhud_frozen),
            string(R.string.perfhud_long_work),
            string(R.string.perfhud_percentiles),
            string(R.string.perfhud_worst),
        )
        tableRule(8)
        frames.screens.sortedByDescending { it.stats.jankyPercent }.forEach { screen ->
            val stats = screen.stats
            row(
                screenName(screen),
                stats.frames.toString(),
                countAndPercent(stats.janky, stats.jankyPercent),
                countAndPercent(stats.slow, stats.slowPercent),
                stats.frozen.toString(),
                stats.longWork.toString(),
                percentiles(stats),
                millis(stats.worstMillis),
            )
        }
        append('\n')
    }

    private fun StringBuilder.memory(snapshot: PerformanceSnapshot) {
        val memory = snapshot.memory ?: return
        heading(2, string(R.string.perfhud_memory))
        table(
            string(R.string.perfhud_java_heap) to
                string(R.string.perfhud_now_peak, megabytes(memory.javaHeapMb), megabytes(memory.peakJavaHeapMb)),
            string(R.string.perfhud_native_heap) to
                string(R.string.perfhud_now_peak, megabytes(memory.nativeHeapMb), megabytes(memory.peakNativeHeapMb)),
            string(R.string.perfhud_pss) to
                string(R.string.perfhud_now_peak, megabytes(memory.pssMb), megabytes(memory.peakPssMb)),
            string(R.string.perfhud_app_pss) to megabytes(memory.appPssMb),
            string(R.string.perfhud_gc) to string(
                R.string.perfhud_gc_value,
                memory.gcCount,
                memory.gcMillis,
                memory.blockingGcCount,
                memory.blockingGcMillis,
            ),
        )
    }

    private fun StringBuilder.startup(snapshot: PerformanceSnapshot) {
        val startup = snapshot.startup
        heading(2, string(R.string.perfhud_startup))
        if (!startup.supported) {
            paragraph(string(R.string.perfhud_startup_unsupported))
            return
        }
        table(
            string(R.string.perfhud_cold_start) to start(startup.cold),
            string(R.string.perfhud_warm_start) to start(startup.warm),
            string(R.string.perfhud_hot_start) to start(startup.hot),
        )
    }

    private fun StringBuilder.network(snapshot: PerformanceSnapshot) {
        val network = snapshot.network
        heading(2, string(R.string.perfhud_network))
        if (network.requests == 0) {
            paragraph(string(R.string.perfhud_network_none))
            return
        }
        table(
            string(R.string.perfhud_requests) to
                string(R.string.perfhud_count_and_part, network.requests, network.failures.toString()),
            string(R.string.perfhud_request_times) to string(
                R.string.perfhud_three_values,
                wholeMillis(network.medianMillis),
                wholeMillis(network.p90Millis),
                wholeMillis(network.slowestMillis),
            ),
            *network.endpoints.map { endpoint ->
                "`" + endpoint.endpoint + "`" to string(
                    R.string.perfhud_endpoint_value,
                    endpoint.requests,
                    wholeMillis(endpoint.medianMillis),
                    wholeMillis(endpoint.slowestMillis),
                )
            }.toTypedArray(),
        )
    }

    private fun StringBuilder.strictMode(snapshot: PerformanceSnapshot) {
        val strictMode = snapshot.strictMode
        heading(2, string(R.string.perfhud_strict_mode))
        if (!strictMode.supported) {
            paragraph(string(R.string.perfhud_strict_mode_unsupported))
            return
        }
        table(
            string(R.string.perfhud_violations) to strictMode.violations.toString(),
            *strictMode.byKind.map { (kind, count) -> kind to count.toString() }.toTypedArray(),
        )
        strictMode.examples.forEach { append("- `").append(resources.strictModeExample(it)).append("`\n") }
        if (strictMode.examples.isNotEmpty()) append('\n')
    }

    private fun StringBuilder.build(snapshot: PerformanceSnapshot) {
        val build = snapshot.build
        heading(2, string(R.string.perfhud_build))
        table(
            string(R.string.perfhud_minified) to yesNo(build.minified),
            string(R.string.perfhud_profile) to string(profileText(build.profile)),
            string(R.string.perfhud_debuggable) to yesNo(build.debuggable),
        )
    }

    private fun StringBuilder.heading(level: Int, text: String) {
        repeat(level) { append('#') }
        append(' ').append(text).append("\n\n")
    }

    private fun StringBuilder.paragraph(text: String) {
        append(text).append("\n\n")
    }

    private fun StringBuilder.table(vararg rows: Pair<String, String>) {
        append("| | |\n|---|---|\n")
        rows.forEach { (label, value) -> row(label, value) }
        append('\n')
    }

    private fun StringBuilder.row(vararg cells: String) {
        append('|')
        cells.forEach { append(' ').append(it.replace("|", "\\|")).append(" |") }
        append('\n')
    }

    private fun StringBuilder.tableRule(columns: Int) {
        append('|')
        repeat(columns) { append("---|") }
        append('\n')
    }

    private fun string(id: Int, vararg args: Any): String = resources.getString(id, *args)

    private fun screenName(screen: ScreenFrames): String = when {
        screen.name != null -> screen.name
        screen.isOther -> string(R.string.perfhud_screen_other)
        else -> string(R.string.perfhud_screen_launch)
    }

    private fun start(record: StartRecord?): String = if (record == null) {
        string(R.string.perfhud_start_none)
    } else {
        string(
            R.string.perfhud_start_value,
            millis(record.initialDisplayMillis),
            record.fullDisplayMillis?.let(::millis) ?: string(R.string.perfhud_start_not_reported),
        )
    }

    private fun percentiles(stats: FrameStats): String = string(
        R.string.perfhud_three_values,
        millis(stats.p50Millis),
        millis(stats.p90Millis),
        millis(stats.p99Millis),
    )

    private fun countAndPercent(count: Int, share: Float): String =
        string(R.string.perfhud_count_and_part, count, percent(share))

    private fun yesNo(value: Boolean): String = string(if (value) R.string.perfhud_yes else R.string.perfhud_no)

    private fun millis(value: Float): String = string(R.string.perfhud_value_millis, decimal(value, 1))

    private fun wholeMillis(value: Long): String = string(R.string.perfhud_value_millis, value.toString())

    private fun percent(value: Float): String = string(R.string.perfhud_value_percent, decimal(value, 2))

    private fun megabytes(value: Float?): String = if (value == null) {
        string(R.string.perfhud_not_measured)
    } else {
        string(R.string.perfhud_value_megabytes, decimal(value, 1))
    }
}

@StringRes
internal fun profileText(status: ProfileStatus): Int = when (status) {
    ProfileStatus.Compiled -> R.string.perfhud_profile_compiled
    ProfileStatus.Enqueued -> R.string.perfhud_profile_enqueued
    ProfileStatus.NotCompiled -> R.string.perfhud_profile_not_compiled
    ProfileStatus.NotInApk -> R.string.perfhud_profile_not_in_apk
    ProfileStatus.NotInProject -> R.string.perfhud_profile_not_in_project
}
