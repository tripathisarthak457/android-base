package com.example.finalafter.crui3.core.designsystem.foundation.glass

import android.content.Context
// allow(designsystem-imports): a verdict kept across launches; no data module sits below this one
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build

/**
 * Remembers that this device could not keep up with live glass, so the next launch starts
 * frosted instead of janking through the same measurement again.
 *
 * The verdict belongs to one system build and one install of the app: a system update changes
 * the GPU driver, and a new build of the app may draw its glass more cheaply, so either measures
 * afresh. The install time stands for the app's version because it also changes when a developer
 * installs a fix under the same version code, which a version code alone would ignore.
 */
internal class GlassVerdictStore(
    private val preferences: SharedPreferences,
    private val build: String,
) {
    val gpuKeepsUp: Boolean
        get() = preferences.getString(FellBehindOn, null) != build

    fun recordGpuFallsBehind() {
        preferences.edit().putString(FellBehindOn, build).apply()
    }

    companion object {
        fun from(context: Context): GlassVerdictStore = GlassVerdictStore(
            preferences = context.getSharedPreferences(FileName, Context.MODE_PRIVATE),
            build = "${Build.FINGERPRINT}@${context.installedAt()}",
        )

        private fun Context.installedAt(): Long {
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION") // The flags overload only exists from Tiramisu.
                packageManager.getPackageInfo(packageName, 0)
            }
            return info.lastUpdateTime
        }

        private const val FileName = "glass_verdict"
        private const val FellBehindOn = "gpu_fell_behind_on"
    }
}
