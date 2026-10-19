package com.base.app.core.perfhud.panel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.base.app.core.designsystem.component.button.AppButton
import com.base.app.core.designsystem.component.button.ButtonSize
import com.base.app.core.designsystem.component.button.ButtonVariant
import com.base.app.core.designsystem.component.container.AppCard
import com.base.app.core.designsystem.component.selection.AppSwitch
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.perfhud.R
import com.base.app.core.perfhud.recording.PerformanceRecorder
import com.base.app.core.perfhud.recording.PerformanceSnapshot
import com.base.app.core.perfhud.report.ReportExporter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * The Performance tab of the inspector: the score, the graphs and every number behind them.
 *
 * Composed only while the tab is open, and everything it draws is read from the recording here,
 * twice a second: closing the tab stops all of it, so the graphs cost the app nothing when nobody
 * is looking at them.
 */
@Composable
internal fun PerformanceTab(recorder: PerformanceRecorder, exporter: ReportExporter, debugBuild: Boolean) {
    val recording by recorder.recording.collectAsStateWithLifecycle()
    var snapshot by remember { mutableStateOf<PerformanceSnapshot?>(null) }
    // Read by the graphs in their draw phase: a new reading redraws them without recomposing them.
    var version by remember { mutableIntStateOf(0) }
    LaunchedEffect(recorder) {
        while (true) {
            snapshot = recorder.snapshot()
            version++
            delay(RefreshMillis)
        }
    }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(AppTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg),
    ) {
        Controls(recorder = recorder, exporter = exporter, recording = recording)
        if (debugBuild) DebugNotice()

        val current = snapshot
        if (!recording && (current == null || current.frames.overall.frames == 0)) {
            AppText(
                text = stringResource(R.string.perfhud_not_recording),
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.contentSecondary,
            )
        }
        if (current != null) {
            ScoreSection(current.score)
            FrameSections(recorder = recorder, snapshot = current, version = { version })
            MemorySection(recorder = recorder, memory = current.memory, version = { version })
            StartupSection(current.startup)
            NetworkSection(current.network)
            StrictModeSection(current.strictMode)
            BuildSection(current.build)
        }
    }
}

@Composable
private fun Controls(recorder: PerformanceRecorder, exporter: ReportExporter, recording: Boolean) {
    val scope = rememberCoroutineScope()
    var exporting by remember { mutableStateOf(false) }
    var exportResult by remember { mutableStateOf<String?>(null) }
    val exportedFormat = stringResource(R.string.perfhud_exported)
    val failedFormat = stringResource(R.string.perfhud_export_failed)

    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
        AppSwitch(
            checked = recording,
            onCheckedChange = recorder::setRecording,
            label = stringResource(R.string.perfhud_live_stats),
            modifier = Modifier.fillMaxWidth(),
        )
        AppText(
            text = stringResource(R.string.perfhud_live_stats_summary),
            style = AppTheme.typography.bodySmall,
            color = AppTheme.colors.contentSecondary,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
            AppButton(
                text = stringResource(if (exporting) R.string.perfhud_exporting else R.string.perfhud_export),
                onClick = {
                    exporting = true
                    scope.launch {
                        exportResult = try {
                            val report = exporter.export(recorder.snapshot())
                            recorder.logExport(report)
                            String.format(exportedFormat, report.json.absolutePath)
                        } catch (failure: IOException) {
                            String.format(failedFormat, failure.message.orEmpty())
                        } finally {
                            exporting = false
                        }
                    }
                },
                enabled = !exporting,
                size = ButtonSize.Small,
            )
            AppButton(
                text = stringResource(R.string.perfhud_reset),
                onClick = recorder::reset,
                variant = ButtonVariant.Secondary,
                size = ButtonSize.Small,
            )
        }
        exportResult?.let { result ->
            SelectionContainer {
                AppText(text = result, style = AppTheme.typography.bodySmall, color = AppTheme.colors.contentSecondary)
            }
        }
    }
}

@Composable
private fun DebugNotice() {
    AppCard(color = AppTheme.colors.warning.subtle, border = null) {
        AppText(
            text = stringResource(R.string.perfhud_debug_title),
            style = AppTheme.typography.titleSmall,
            color = AppTheme.colors.warning.content,
        )
        AppText(
            text = stringResource(R.string.perfhud_debug_body),
            style = AppTheme.typography.bodySmall,
            color = AppTheme.colors.contentPrimary,
            modifier = Modifier.padding(top = AppTheme.spacing.xs),
        )
    }
}

private const val RefreshMillis = 500L
