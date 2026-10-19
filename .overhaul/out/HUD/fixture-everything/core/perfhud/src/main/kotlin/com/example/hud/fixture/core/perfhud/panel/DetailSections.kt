package com.example.hud.fixture.core.perfhud.panel

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.hud.fixture.core.designsystem.component.text.AppMonoText
import com.example.hud.fixture.core.designsystem.component.text.AppText
import com.example.hud.fixture.core.designsystem.theme.AppTheme
import com.example.hud.fixture.core.perfhud.R
import com.example.hud.fixture.core.perfhud.checks.BuildFacts
import com.example.hud.fixture.core.perfhud.checks.StrictModeSummary
import com.example.hud.fixture.core.perfhud.memory.MemorySummary
import com.example.hud.fixture.core.perfhud.network.NetworkTimings
import com.example.hud.fixture.core.perfhud.panel.graph.MemoryGraph
import com.example.hud.fixture.core.perfhud.recording.PerformanceRecorder
import com.example.hud.fixture.core.perfhud.report.profileText
import com.example.hud.fixture.core.perfhud.score.decimal
import com.example.hud.fixture.core.perfhud.startup.StartRecord
import com.example.hud.fixture.core.perfhud.startup.StartupSummary

@Composable
internal fun MemorySection(recorder: PerformanceRecorder, memory: MemorySummary?, version: () -> Int) {
    Section(title = R.string.perfhud_memory, caption = R.string.perfhud_memory_caption) {
        MemoryGraph(memory = recorder.memory, version = version)
        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
            Legend(AppTheme.colors.accent, R.string.perfhud_legend_java)
            Legend(AppTheme.colors.secondary, R.string.perfhud_legend_native)
            Legend(AppTheme.colors.tertiary, R.string.perfhud_legend_pss)
        }
    }
    if (memory == null) return
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
        ValueRow(R.string.perfhud_java_heap, nowAndPeak(memory.javaHeapMb, memory.peakJavaHeapMb))
        ValueRow(R.string.perfhud_native_heap, nowAndPeak(memory.nativeHeapMb, memory.peakNativeHeapMb))
        ValueRow(R.string.perfhud_pss, nowAndPeak(memory.pssMb, memory.peakPssMb))
        ValueRow(R.string.perfhud_app_pss, megabytes(memory.appPssMb))
        ValueRow(
            R.string.perfhud_gc,
            stringResource(
                R.string.perfhud_gc_value,
                memory.gcCount,
                memory.gcMillis,
                memory.blockingGcCount,
                memory.blockingGcMillis,
            ),
        )
    }
}

@Composable
internal fun StartupSection(startup: StartupSummary) {
    Section(title = R.string.perfhud_startup) {
        if (!startup.supported) {
            Caption(R.string.perfhud_startup_unsupported)
            return@Section
        }
        ValueRow(R.string.perfhud_cold_start, start(startup.cold))
        ValueRow(R.string.perfhud_warm_start, start(startup.warm))
        ValueRow(R.string.perfhud_hot_start, start(startup.hot))
    }
}

@Composable
internal fun NetworkSection(network: NetworkTimings) {
    Section(title = R.string.perfhud_network) {
        if (network.requests == 0) {
            Caption(R.string.perfhud_network_none)
            return@Section
        }
        ValueRow(
            R.string.perfhud_requests,
            stringResource(R.string.perfhud_count_and_part, network.requests, network.failures.toString()),
        )
        ValueRow(
            R.string.perfhud_request_times,
            stringResource(
                R.string.perfhud_three_values,
                stringResource(R.string.perfhud_value_millis, network.medianMillis.toString()),
                stringResource(R.string.perfhud_value_millis, network.p90Millis.toString()),
                stringResource(R.string.perfhud_value_millis, network.slowestMillis.toString()),
            ),
        )
        network.endpoints.forEach { endpoint ->
            ValueRow(
                label = endpoint.endpoint,
                value = stringResource(
                    R.string.perfhud_endpoint_value,
                    endpoint.requests,
                    stringResource(R.string.perfhud_value_millis, endpoint.medianMillis.toString()),
                    stringResource(R.string.perfhud_value_millis, endpoint.slowestMillis.toString()),
                ),
            )
        }
    }
}

@Composable
internal fun StrictModeSection(strictMode: StrictModeSummary) {
    Section(title = R.string.perfhud_strict_mode) {
        if (!strictMode.supported) {
            Caption(R.string.perfhud_strict_mode_unsupported)
            return@Section
        }
        ValueRow(R.string.perfhud_violations, strictMode.violations.toString())
        strictMode.byKind.forEach { (kind, count) -> ValueRow(label = kind, value = count.toString()) }
        if (strictMode.examples.isNotEmpty()) {
            SelectionContainer {
                Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xxs)) {
                    strictMode.examples.forEach { example ->
                        AppMonoText(
                            text = stringResource(
                                R.string.perfhud_strict_mode_example,
                                example.kind,
                                example.frame ?: stringResource(R.string.perfhud_strict_mode_unknown_frame),
                            ),
                            color = AppTheme.colors.contentSecondary,
                            maxLines = 2,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun BuildSection(build: BuildFacts) {
    Section(title = R.string.perfhud_build) {
        ValueRow(R.string.perfhud_minified, yesNo(build.minified))
        ValueRow(R.string.perfhud_profile, stringResource(profileText(build.profile)))
        ValueRow(R.string.perfhud_debuggable, yesNo(build.debuggable))
    }
}

/** A titled block: a heading, an optional caption, and its content. */
@Composable
internal fun Section(
    @StringRes title: Int,
    @StringRes caption: Int? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
    ) {
        AppText(
            text = stringResource(title),
            style = AppTheme.typography.titleSmall,
            color = AppTheme.colors.contentPrimary,
        )
        if (caption != null) Caption(caption)
        content()
    }
}

@Composable
internal fun ValueRow(@StringRes label: Int, value: String) {
    ValueRow(label = stringResource(label), value = value)
}

@Composable
internal fun ValueRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        AppText(
            text = label,
            style = AppTheme.typography.bodySmall,
            color = AppTheme.colors.contentSecondary,
            modifier = Modifier.weight(1f),
        )
        AppMonoText(
            text = value,
            color = AppTheme.colors.contentPrimary,
            maxLines = 2,
            modifier = Modifier.weight(ValueWeight),
        )
    }
}

@Composable
private fun Caption(@StringRes text: Int) {
    AppText(
        text = stringResource(text),
        style = AppTheme.typography.caption,
        color = AppTheme.colors.contentTertiary,
    )
}

@Composable
private fun Legend(color: Color, @StringRes label: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
    ) {
        Box(modifier = Modifier.size(LegendDot).background(color, AppTheme.shapes.pill))
        AppText(
            text = stringResource(label),
            style = AppTheme.typography.caption,
            color = AppTheme.colors.contentSecondary,
        )
    }
}

@Composable
private fun start(record: StartRecord?): String = if (record == null) {
    stringResource(R.string.perfhud_start_none)
} else {
    stringResource(
        R.string.perfhud_start_value,
        millis(record.initialDisplayMillis),
        record.fullDisplayMillis?.let { millis(it) } ?: stringResource(R.string.perfhud_start_not_reported),
    )
}

@Composable
private fun nowAndPeak(now: Float?, peak: Float?): String =
    stringResource(R.string.perfhud_now_peak, megabytes(now), megabytes(peak))

@Composable
private fun megabytes(value: Float?): String = if (value == null) {
    stringResource(R.string.perfhud_not_measured)
} else {
    stringResource(R.string.perfhud_value_megabytes, decimal(value, 1))
}

@Composable
private fun yesNo(value: Boolean): String = stringResource(if (value) R.string.perfhud_yes else R.string.perfhud_no)

private val LegendDot = 8.dp
private const val ValueWeight = 1.4f
