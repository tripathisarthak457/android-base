package com.example.cmp.core.perfhud.report

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.cmp.core.common.coroutines.ApplicationScope
import com.example.cmp.core.perfhud.recording.PerformanceRecorder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * The adb path to the overlay, for scripts on a debug build:
 *
 * ```
 * adb shell run-as <applicationId> am broadcast --user 0 \
 *     -n <applicationId>/com.example.cmp.core.perfhud.report.ReportReceiver -a <action>
 * ```
 *
 * with [ACTION_EXPORT] to write a report (the result data is the JSON file's path, or the error
 * with result code [RESULT_FAILED]), or [ACTION_RECORD] with `--ez recording true|false` to switch
 * recording.
 *
 * Declared in the debug manifest only, and not exported, so no other app can reach it: `run-as`
 * gives adb the app's own uid, and only on a debuggable build. `--user` names the app's own user
 * because a broadcast to the default, the current user, needs a cross-user permission the app's
 * uid does not hold. A release-like dev build exports its report from the Perf tab instead, and a
 * shippable release has no receiver at all.
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
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (failure: Exception) {
                        // The script on the other end learns of any failure from the result, and
                        // nothing escapes into the application scope.
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
        const val ACTION_EXPORT = "com.example.cmp.core.perfhud.action.EXPORT"
        const val ACTION_RECORD = "com.example.cmp.core.perfhud.action.RECORD"
        const val EXTRA_RECORDING = "recording"
        const val RESULT_FAILED = 1
    }
}
