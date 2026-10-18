package com.example.hud.standard.core.perfhud.recording

import android.os.Build
import com.example.hud.standard.core.perfhud.checks.BuildFacts
import com.example.hud.standard.core.perfhud.checks.StrictModeSummary
import com.example.hud.standard.core.perfhud.frame.FrameSummary
import com.example.hud.standard.core.perfhud.memory.MemorySummary
import com.example.hud.standard.core.perfhud.network.NetworkTimings
import com.example.hud.standard.core.perfhud.score.OptimisationScore
import com.example.hud.standard.core.perfhud.startup.StartupSummary
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
