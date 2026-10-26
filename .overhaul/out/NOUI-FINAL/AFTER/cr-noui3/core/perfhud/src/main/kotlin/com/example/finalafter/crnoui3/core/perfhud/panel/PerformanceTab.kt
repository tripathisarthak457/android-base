package com.example.finalafter.crnoui3.core.perfhud.panel

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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.finalafter.crnoui3.core.designsystem.theme.AppTheme
import com.example.finalafter.crnoui3.core.devtools.kit.AppButton
import com.example.finalafter.crnoui3.core.devtools.kit.AppCard
import com.example.finalafter.crnoui3.core.devtools.kit.AppScrollDefaults
import com.example.finalafter.crnoui3.core.devtools.kit.AppText
import com.example.finalafter.crnoui3.core.devtools.kit.AppToggleRow
import com.example.finalafter.crnoui3.core.devtools.kit.ButtonSize
import com.example.finalafter.crnoui3.core.devtools.kit.ButtonVariant
import com.example.finalafter.crnoui3.core.perfhud.R
import com.example.finalafter.crnoui3.core.perfhud.recording.PerformanceRecorder
import com.example.finalafter.crnoui3.core.perfhud.recording.PerformanceSnapshot
import com.example.finalafter.crnoui3.core.perfhud.report.ReportExporter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The Performance tab of the inspector: the score, the graphs and every number behind them.
 *
 * Composed only while the tab is open, and everything it draws is read from the recording here,
 * twice a second while the app is in front: closing the tab stops all of it, so the graphs cost the
 * app nothing when nobody is looking at them.
 */
@Composable
internal fun PerformanceTab(recorder: PerformanceRecorder, exporter: ReportExporter, debugBuild: Boolean) {
    val recording by recorder.recording.collectAsStateWithLifecycle()
    var snapshot by remember { mutableStateOf<PerformanceSnapshot?>(null) }
    // Read by the graphs in their draw phase: a new reading redraws them without recomposing them.
    var version by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    // A panel left open while the app goes to the background would otherwise keep taking snapshots.
    LaunchedEffect(recorder, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                snapshot = recorder.snapshot()
                version++
                delay(RefreshMillis)
            }
        }
    }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState(), flingBehavior = AppScrollDefaults.flingBehavior())
            .padding(AppTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg),
    ) {
        Controls(recorder = recorder, exporter = exporter, recording = recording)
        if (debugBuild) DebugNotice()

        val current = snapshot
        if (!recording && (current == null || current.frames.overall.frames == 0)) {
            AppText(
                text = stringResource(R.string.perfhud_not_recording),
                style = AppTheme.typography.callout,
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
        AppToggleRow(
            title = stringResource(R.string.perfhud_live_stats),
            checked = recording,
            onCheckedChange = recorder::setRecording,
            modifier = Modifier.fillMaxWidth(),
        )
        AppText(
            text = stringResource(R.string.perfhud_live_stats_summary),
            style = AppTheme.typography.subheadline,
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
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (failure: Exception) {
                            // A failed export is reported where it was asked for; nothing escapes to
                            // crash the app being inspected.
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
                variant = ButtonVariant.Tinted,
                size = ButtonSize.Small,
            )
        }
        exportResult?.let { result ->
            SelectionContainer {
                AppText(text = result, style = AppTheme.typography.subheadline, color = AppTheme.colors.contentSecondary)
            }
        }
    }
}

@Composable
private fun DebugNotice() {
    AppCard(color = AppTheme.colors.warning.subtle, border = null) {
        AppText(
            text = stringResource(R.string.perfhud_debug_title),
            style = AppTheme.typography.footnote,
            color = AppTheme.colors.warning.content,
        )
        AppText(
            text = stringResource(R.string.perfhud_debug_body),
            style = AppTheme.typography.subheadline,
            color = AppTheme.colors.contentPrimary,
            modifier = Modifier.padding(top = AppTheme.spacing.xs),
        )
    }
}

private const val RefreshMillis = 500L
