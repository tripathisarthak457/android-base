package com.example.cmp.core.designsystem.foundation.glass

import android.os.PowerManager
import org.junit.Assert.assertEquals
import org.junit.Test

class GlassPolicyTest {

    private val healthy = GlassInputs(
        sdkInt = 35,
        lowRamDevice = false,
        powerSave = false,
        thermalStatus = PowerManager.THERMAL_STATUS_NONE,
        gpuKeepsUp = true,
        reduceTransparency = false,
    )

    @Test
    fun `a healthy Tiramisu or later device gets the lens`() {
        assertEquals(GlassQuality.Live, glassQuality(healthy))
        assertEquals(GlassQuality.Live, glassQuality(healthy.copy(sdkInt = 33)))
    }

    @Test
    fun `S and S V2 blur without the lens, which is an AGSL shader`() {
        assertEquals(GlassQuality.LiveNoLens, glassQuality(healthy.copy(sdkInt = 31)))
        assertEquals(GlassQuality.LiveNoLens, glassQuality(healthy.copy(sdkInt = 32)))
    }

    @Test
    fun `below S nothing samples, since Kyant would draw the scene unblurred`() {
        assertEquals(GlassQuality.Frosted, glassQuality(healthy.copy(sdkInt = 30)))
        assertEquals(GlassQuality.Frosted, glassQuality(healthy.copy(sdkInt = 26)))
    }

    @Test
    fun `low RAM, battery saver and a GPU that fell behind each frost the glass`() {
        assertEquals(GlassQuality.Frosted, glassQuality(healthy.copy(lowRamDevice = true)))
        assertEquals(GlassQuality.Frosted, glassQuality(healthy.copy(powerSave = true)))
        assertEquals(GlassQuality.Frosted, glassQuality(healthy.copy(gpuKeepsUp = false)))
    }

    @Test
    fun `a warm device loses the lens and a hot one the blur`() {
        assertEquals(GlassQuality.Live, thermal(PowerManager.THERMAL_STATUS_LIGHT))
        assertEquals(GlassQuality.LiveNoLens, thermal(PowerManager.THERMAL_STATUS_MODERATE))
        assertEquals(GlassQuality.Frosted, thermal(PowerManager.THERMAL_STATUS_SEVERE))
        assertEquals(GlassQuality.Frosted, thermal(PowerManager.THERMAL_STATUS_SHUTDOWN))
    }

    @Test
    fun `reduce transparency wins over everything, even a device that could not blur`() {
        val reduced = healthy.copy(reduceTransparency = true)

        assertEquals(GlassQuality.Solid, glassQuality(reduced))
        assertEquals(GlassQuality.Solid, glassQuality(reduced.copy(sdkInt = 26)))
        assertEquals(GlassQuality.Solid, glassQuality(reduced.copy(powerSave = true)))
    }

    @Test
    fun `only the live tiers sample the scene, and only Live refracts`() {
        assertEquals(
            listOf(true, true, false, false),
            GlassQuality.entries.map { it.samplesScene },
        )
        assertEquals(
            listOf(true, false, false, false),
            GlassQuality.entries.map { it.refracts },
        )
    }

    private fun thermal(status: Int): GlassQuality =
        glassQuality(healthy.copy(thermalStatus = status))
}
