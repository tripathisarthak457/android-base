package com.base.app.core.perfhud.report

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.base.app.core.common.coroutines.ApplicationScope
import com.base.app.core.perfhud.recording.PerformanceRecorder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

/**
 * The adb path to the overlay, for scripts and CI:
 *
 * ```
 * adb shell am broadcast -n <applicationId>/com.base.app.core.perfhud.report.ReportReceiver -a <action>
 * ```
 *
 * with [ACTION_EXPORT] to write a report (the result data is the JSON file's path, or the error
 * with result code [RESULT_FAILED]), or
 * [ACTION_RECORD] with `--ez recording true|false` to switch recording.
 *
 * Exported, because adb's shell is another app as far as the system is concerned, and guarded by
 * `android.permission.DUMP` in the manifest: a signature, privileged and development permission
 * the shell holds and no installed app can, which is how androidx's `ProfileInstallReceiver`
 * takes its adb broadcast too. It exists only in the builds that carry this module, never in a
 * shippable release.
 */
@AndroidEntryPoint
internal class ReportReceiver : BroadcastReceiver() {

    @Inject
    lateinit var recorder: PerformanceRecorder

    @Inject
    lateinit var exporter: ReportExporter

    @Inject
    lateinit var scope: ApplicationScope

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_RECORD -> {
                if (!intent.hasExtra(EXTRA_RECORDING)) return
                recorder.setRecording(intent.getBooleanExtra(EXTRA_RECORDING, false))
            }
            ACTION_EXPORT -> {
                val pending = goAsync()
                scope.launch {
                    try {
                        val report = exporter.export(recorder.snapshot())
                        recorder.logExport(report)
                        pending.resultData = report.json.absolutePath
                    } catch (failure: IOException) {
                        pending.resultCode = RESULT_FAILED
                        pending.resultData = failure.toString()
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_EXPORT = "com.base.app.core.perfhud.action.EXPORT"
        const val ACTION_RECORD = "com.base.app.core.perfhud.action.RECORD"
        const val EXTRA_RECORDING = "recording"
        const val RESULT_FAILED = 1
    }
}
