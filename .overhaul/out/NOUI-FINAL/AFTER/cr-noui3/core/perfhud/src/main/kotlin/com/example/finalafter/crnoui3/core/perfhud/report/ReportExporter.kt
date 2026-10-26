package com.example.finalafter.crnoui3.core.perfhud.report

import android.content.Context
import com.example.finalafter.crnoui3.core.common.coroutines.AppDispatchers
import com.example.finalafter.crnoui3.core.perfhud.recording.PerformanceSnapshot
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.withContext

/**
 * Writes a snapshot as `perf-report-<time>.json` and `.md` into the app's external files
 * directory, under `perfhud/`: no permission is needed to write there, and `adb pull` can read it
 * from a release build, which `run-as` cannot.
 */
internal class ReportExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: AppDispatchers,
) {

    suspend fun export(snapshot: PerformanceSnapshot): ExportedReport = withContext(dispatchers.io) {
        val now = ZonedDateTime.now()
        val generatedAt = now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        val directory = File(context.getExternalFilesDir(null) ?: context.filesDir, DIRECTORY)
        directory.mkdirs()
        val name = FILE_PREFIX + now.format(FILE_TIME)
        val json = File(directory, "$name.json")
        val markdown = File(directory, "$name.md")
        json.writeText(PerformanceReport.of(snapshot, generatedAt, context.resources).toJson())
        markdown.writeText(MarkdownReport(context.resources).write(snapshot, generatedAt))
        ExportedReport(json = json, markdown = markdown)
    }

    private companion object {
        const val DIRECTORY = "perfhud"
        const val FILE_PREFIX = "perf-report-"
        val FILE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.ROOT)
    }
}

internal data class ExportedReport(val json: File, val markdown: File)
