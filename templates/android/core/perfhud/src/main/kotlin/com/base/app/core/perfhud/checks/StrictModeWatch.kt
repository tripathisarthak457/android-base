package com.base.app.core.perfhud.checks

import android.os.Build
import android.os.StrictMode
import android.os.strictmode.Violation
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.RequiresApi
import kotlinx.serialization.Serializable
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton

/**
 * StrictMode's main-thread checks while recording, counted: disk and network on the main thread,
 * unbuffered I/O, resource mismatches and calls marked slow. The policy the app had is put back on
 * [stop]. Counting needs a penalty listener, which arrived in API 28; earlier releases report
 * nothing rather than a count that cannot be taken.
 *
 * [start] and [stop] on the main thread: a thread policy belongs to the thread that sets it.
 */
@Singleton
internal class StrictModeWatch @Inject constructor() {

    private val lock = Any()
    private val byKind = LinkedHashMap<String, Int>()
    private val examples = ArrayList<String>()
    private var previous: StrictMode.ThreadPolicy? = null

    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.P)
    val supported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

    fun start() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P || previous != null) return
        val current = StrictMode.getThreadPolicy()
        previous = current
        StrictMode.setThreadPolicy(watching(current))
    }

    fun stop() {
        StrictMode.setThreadPolicy(previous ?: return)
        previous = null
    }

    fun clear() = synchronized(lock) {
        byKind.clear()
        examples.clear()
    }

    fun summary(): StrictModeSummary = synchronized(lock) {
        StrictModeSummary(
            supported = supported,
            violations = byKind.values.sum(),
            byKind = byKind.toMap(),
            examples = examples.toList(),
        )
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun watching(base: StrictMode.ThreadPolicy): StrictMode.ThreadPolicy =
        StrictMode.ThreadPolicy.Builder(base)
            .detectDiskReads()
            .detectDiskWrites()
            .detectNetwork()
            .detectCustomSlowCalls()
            .detectResourceMismatches()
            .detectUnbufferedIo()
            // Run on the thread that broke the rule, after the fact: counting is all that happens.
            .penaltyListener(Executor(Runnable::run), ::record)
            .build()

    @RequiresApi(Build.VERSION_CODES.P)
    private fun record(violation: Violation) = synchronized(lock) {
        val kind = violation.javaClass.simpleName.removeSuffix(VIOLATION_SUFFIX)
        byKind[kind] = (byKind[kind] ?: 0) + 1
        if (examples.size < MAX_EXAMPLES) examples += kind + " at " + firstAppFrame(violation)
    }

    /** The first frame of the stack that is not the platform's or a library's: where to look. */
    private fun firstAppFrame(violation: Violation): String {
        val frame = violation.stackTrace.firstOrNull { element ->
            PLATFORM_PREFIXES.none { element.className.startsWith(it) }
        } ?: violation.stackTrace.firstOrNull()
        return frame?.toString() ?: UNKNOWN_FRAME
    }

    private companion object {
        const val VIOLATION_SUFFIX = "Violation"
        const val MAX_EXAMPLES = 10
        const val UNKNOWN_FRAME = "an unknown frame"
        val PLATFORM_PREFIXES = listOf(
            "android.", "androidx.", "java.", "javax.", "kotlin.", "kotlinx.", "dalvik.", "com.android.",
            "libcore.", "sun.", "okhttp3.", "okio.", "io.ktor.",
        )
    }
}

@Serializable
internal data class StrictModeSummary(
    val supported: Boolean,
    val violations: Int,
    /** By the violation's kind, such as `DiskRead` or `Network`. */
    val byKind: Map<String, Int>,
    /** The first few, each with the first frame of the app's own code on its stack. */
    val examples: List<String>,
)
