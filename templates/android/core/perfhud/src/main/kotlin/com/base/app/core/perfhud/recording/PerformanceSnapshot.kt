package com.base.app.core.perfhud.recording

import android.os.Build
import com.base.app.core.perfhud.checks.BuildFacts
import com.base.app.core.perfhud.checks.StrictModeSummary
import com.base.app.core.perfhud.frame.FrameSummary
import com.base.app.core.perfhud.memory.MemorySummary
import com.base.app.core.perfhud.network.NetworkTimings
import com.base.app.core.perfhud.score.OptimisationScore
import com.base.app.core.perfhud.startup.StartupSummary
import kotlinx.serialization.Serializable

/** Everything recorded so far, summed up at one moment: what the tab shows and the report says. */
internal data class PerformanceSnapshot(
    val frames: FrameSummary,
    val memory: MemorySummary?,
    val startup: StartupSummary,
    val build: BuildFacts,
    val strictMode: StrictModeSummary,
    val network: NetworkTimings,
    val score: OptimisationScore,
    val recordedMillis: Long,
    val app: AppFacts,
    val device: DeviceFacts,
)

@Serializable
internal data class AppFacts(
    val applicationId: String,
    val versionName: String,
    val environment: String,
    val debugBuild: Boolean,
)

@Serializable
internal data class DeviceFacts(
    val manufacturer: String,
    val model: String,
    val release: String,
    val sdk: Int,
    /** The rates the display offers, highest first. */
    val refreshRatesHz: List<Float>,
) {
    companion object {
        fun current(refreshRatesHz: List<Float>) = DeviceFacts(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            release = Build.VERSION.RELEASE,
            sdk = Build.VERSION.SDK_INT,
            refreshRatesHz = refreshRatesHz,
        )
    }
}
