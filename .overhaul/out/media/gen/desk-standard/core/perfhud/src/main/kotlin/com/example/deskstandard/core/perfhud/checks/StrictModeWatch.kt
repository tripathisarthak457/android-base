package com.example.deskstandard.core.perfhud.checks

import android.content.res.Resources
import android.os.Build
import android.os.StrictMode
import android.os.strictmode.Violation
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.RequiresApi
import com.example.deskstandard.core.common.coroutines.AppDispatchers
import com.example.deskstandard.core.perfhud.R
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.asExecutor
import kotlinx.serialization.Serializable

/**
 * StrictMode's main-thread checks while recording, counted: disk and network on the main thread,
 * unbuffered I/O, resource mismatches and calls marked slow. The policy the app had is put back on
 * [stop]. Counting needs a penalty listener, which arrived in API 28; earlier releases report
 * nothing rather than a count that cannot be taken.
 *
 * The checks are added to the app's own policy, penalties included, so the app keeps the checks and
 * penalties it asked for. An app whose policy has `penaltyDeath` therefore also dies of the checks
 * added here, and one that sets its own policy while recording replaces these checks until the next
 * [start]. A penalty listener of the app's own is the exception: a policy holds one listener, and
 * the platform offers no way to read the app's to pass violations on, so it hears nothing while
 * recording.
 *
 * [start] and [stop] on the main thread: a thread policy belongs to the thread that sets it.
 */
@Singleton
internal class StrictModeWatch @Inject constructor(dispatchers: AppDispatchers) {

    // Counting needs nothing from the thread that broke the rule, usually the main one: the
    // violation carries its stack.
    private val counting: Executor = dispatchers.default.asExecutor()

    private val lock = Any()
    private val byKind = LinkedHashMap<String, Int>()
    private val examples = ArrayList<StrictModeExample>()
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
            .penaltyListener(counting, ::record)
            .build()

    @RequiresApi(Build.VERSION_CODES.P)
    private fun record(violation: Violation) = synchronized(lock) {
        val kind = violation.javaClass.simpleName.removeSuffix(VIOLATION_SUFFIX)
        byKind[kind] = (byKind[kind] ?: 0) + 1
        if (examples.size < MAX_EXAMPLES) examples += StrictModeExample(kind, firstAppFrame(violation))
    }

    /** The first frame of the stack that is not the platform's or a library's: where to look. */
    private fun firstAppFrame(violation: Violation): String? {
        val frame = violation.stackTrace.firstOrNull { element ->
            PLATFORM_PREFIXES.none { element.className.startsWith(it) }
        } ?: violation.stackTrace.firstOrNull()
        return frame?.toString()
    }

    private companion object {
        const val VIOLATION_SUFFIX = "Violation"
        const val MAX_EXAMPLES = 10
        // The platform's and the libraries the template can bring, Play services and Firebase
        // included: a violation inside one is reported at the app's call into it.
        val PLATFORM_PREFIXES = listOf(
            "android.", "androidx.", "java.", "javax.", "kotlin.", "kotlinx.", "dalvik.", "com.android.",
            "libcore.", "sun.", "okhttp3.", "okio.", "io.ktor.", "com.google.", "dagger.",
            "com.airbnb.lottie.", "leakcanary.", "shark.",
        )
    }
}

@Serializable
internal data class StrictModeSummary(
    val supported: Boolean,
    val violations: Int,
    /** By the violation's kind, such as `DiskRead` or `Network`. */
    val byKind: Map<String, Int>,
    /** The first few, in the order they happened. */
    val examples: List<StrictModeExample>,
)

/**
 * One violation: its [kind], as in [StrictModeSummary.byKind], and the first frame of the app's
 * own code on its stack, or of the stack when none of it is the app's; null for an empty stack.
 */
@Serializable
internal data class StrictModeExample(val kind: String, val frame: String?)

/** [example] as a line the panel and the report print. */
internal fun Resources.strictModeExample(example: StrictModeExample): String = getString(
    R.string.perfhud_strict_mode_example,
    example.kind,
    example.frame ?: getString(R.string.perfhud_strict_mode_unknown_frame),
)
