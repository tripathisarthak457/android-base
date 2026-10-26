package com.example.finalafter.crui2.core.perfhud.memory

import android.os.Debug
import javax.inject.Inject

/**
 * Reads the process's memory from the platform. Call off the main thread: [read] with PSS walks
 * every memory mapping of the process.
 */
internal class MemorySampler @Inject constructor() {

    /** The heaps and GC counters, which cost a counter read each; PSS and graphics only if [withPss]. */
    fun read(withPss: Boolean): MemoryReading {
        val runtime = Runtime.getRuntime()
        var pss = Float.NaN
        var graphics = Float.NaN
        if (withPss) {
            val info = Debug.MemoryInfo()
            Debug.getMemoryInfo(info)
            pss = info.statMb(TOTAL_PSS)
            graphics = info.statMb(GRAPHICS)
        }
        return MemoryReading(
            javaHeapMb = (runtime.totalMemory() - runtime.freeMemory()) / BYTES_PER_MB,
            nativeHeapMb = Debug.getNativeHeapAllocatedSize() / BYTES_PER_MB,
            pssMb = pss,
            graphicsMb = graphics,
            gc = GcCounters(
                count = runtimeStat(GC_COUNT),
                millis = runtimeStat(GC_TIME),
                blockingCount = runtimeStat(BLOCKING_GC_COUNT),
                blockingMillis = runtimeStat(BLOCKING_GC_TIME),
            ),
        )
    }

    private fun Debug.MemoryInfo.statMb(name: String): Float =
        getMemoryStat(name)?.toFloatOrNull()?.div(KB_PER_MB) ?: Float.NaN

    private fun runtimeStat(name: String): Long = Debug.getRuntimeStat(name)?.toLongOrNull() ?: 0L

    private companion object {
        const val BYTES_PER_MB = 1024f * 1024f
        const val KB_PER_MB = 1024f
        const val TOTAL_PSS = "summary.total-pss"
        const val GRAPHICS = "summary.graphics"
        const val GC_COUNT = "art.gc.gc-count"
        const val GC_TIME = "art.gc.gc-time"
        const val BLOCKING_GC_COUNT = "art.gc.blocking-gc-count"
        const val BLOCKING_GC_TIME = "art.gc.blocking-gc-time"
    }
}
