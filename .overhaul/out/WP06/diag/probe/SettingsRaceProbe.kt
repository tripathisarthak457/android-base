package com.example.wp06.standardnofirebase.core.applock

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.wp06.standardnofirebase.core.common.coroutines.ApplicationScope
import com.example.wp06.standardnofirebase.core.datastore.settings.AppSettingsStore
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File
import java.nio.file.Files

// Diagnostic only (WP06 run 15): what happens on Windows when AppSettingsStore's eager read races the first edit.
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class SettingsRaceProbe {
    @Test
    fun probe() {
        val out = File(System.getenv("PROBE_OUT") ?: "D:/projects/tools/.overhaul/out/WP06/diag/probe/result.txt")
        val report = StringBuilder()
        var stalls = 0
        var failedEdits = 0
        val runs = (System.getenv("PROBE_RUNS") ?: "400").toInt()
        repeat(runs) { i ->
            val dir = Files.createTempDirectory("probe").toFile()
            val errors = java.util.Collections.synchronizedList(mutableListOf<Throwable>())
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, e -> errors += e })
            val dataStore = PreferenceDataStoreFactory.create(scope = scope) { File(dir, "settings.preferences_pb") }
            val settings = AppSettingsStore(dataStore, ApplicationScope(scope))
            val result = runBlocking {
                val edit = withTimeoutOrNull(3_000) { settings.setAppLockEnabled(true); "edited" }
                    ?: return@runBlocking "edit-stalled"
                withTimeoutOrNull(3_000) { settings.settings.first { it.appLockEnabled }; edit } ?: "read-stalled"
            }
            if (result != "edited") {
                if (result == "read-stalled") stalls++ else failedEdits++
                report.append("run $i: $result; current=${settings.current}; file=${File(dir, "settings.preferences_pb").length()} B\n")
                errors.forEach { report.append(it.stackTraceToString()).append('\n') }
            } else if (errors.isNotEmpty()) {
                report.append("run $i: ok but errors:\n")
                errors.forEach { report.append(it.stackTraceToString()).append('\n') }
            }
            scope.cancel()
            dir.deleteRecursively()
        }
        report.insert(0, "runs=$runs readStalls=$stalls editStalls=$failedEdits\n")
        out.writeText(report.toString())
    }
}
