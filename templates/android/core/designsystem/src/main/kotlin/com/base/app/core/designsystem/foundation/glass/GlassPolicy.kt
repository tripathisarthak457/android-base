package com.base.app.core.designsystem.foundation.glass

import android.os.Build
import android.os.PowerManager

/** Everything the tier depends on, as plain values, so the policy is a pure function. */
internal data class GlassInputs(
    val sdkInt: Int,
    val lowRamDevice: Boolean,
    val powerSave: Boolean,
    /** A `PowerManager.THERMAL_STATUS_*` value; `NONE` below API 29, which has no thermal API. */
    val thermalStatus: Int,
    val gpuKeepsUp: Boolean,
    val reduceTransparency: Boolean,
)

/**
 * The tier for [inputs], as the table on [GlassQuality] states it.
 *
 * There is no media performance class rule: most mid-range phones declare none, so it would demote
 * them without measuring anything, while the GPU verdict measures the device that is running.
 */
internal fun glassQuality(inputs: GlassInputs): GlassQuality = with(inputs) {
    when {
        reduceTransparency -> GlassQuality.Solid
        // Below S, Kyant's effects are no-ops and a sampling surface would show the scene sharp.
        sdkInt < Build.VERSION_CODES.S -> GlassQuality.Frosted
        lowRamDevice || powerSave || !gpuKeepsUp -> GlassQuality.Frosted
        thermalStatus >= PowerManager.THERMAL_STATUS_SEVERE -> GlassQuality.Frosted
        // The lens is an AGSL shader, which only Tiramisu runs.
        sdkInt < Build.VERSION_CODES.TIRAMISU -> GlassQuality.LiveNoLens
        thermalStatus >= PowerManager.THERMAL_STATUS_MODERATE -> GlassQuality.LiveNoLens
        else -> GlassQuality.Live
    }
}
