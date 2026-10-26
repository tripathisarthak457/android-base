package com.example.cmp.core.designsystem.foundation.glass

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [RobolectricSdk])
class GlassQualityTest {

    @get:Rule
    val rule = createComposeRule()

    private val context: Context = RuntimeEnvironment.getApplication()
    private val power = shadowOf(context.getSystemService(PowerManager::class.java))

    @Test
    fun `a healthy device gets live glass with the lens`() {
        assertEquals(GlassQuality.Live, tier()())
    }

    @Test
    fun `battery saver frosts the glass, and turning it off brings the glass back`() {
        val tier = tier()

        setPowerSave(true)
        val saving = tier()
        setPowerSave(false)

        assertEquals(GlassQuality.Frosted, saving)
        assertEquals(GlassQuality.Live, tier())
    }

    @Test
    fun `a device that warmed up keeps its lower tier after it cools down`() {
        val tier = tier()

        power.setCurrentThermalStatus(PowerManager.THERMAL_STATUS_MODERATE)
        val warm = tier()
        power.setCurrentThermalStatus(PowerManager.THERMAL_STATUS_NONE)
        val cooled = tier()
        power.setCurrentThermalStatus(PowerManager.THERMAL_STATUS_SEVERE)

        assertEquals(GlassQuality.LiveNoLens, warm)
        assertEquals(GlassQuality.LiveNoLens, cooled)
        assertEquals(GlassQuality.Frosted, tier())
    }

    @Test
    fun `reduce transparency makes the chrome solid`() {
        assertEquals(GlassQuality.Solid, tier(reduceTransparency = true)())
    }

    @Test
    fun `a low-RAM device gets frosted glass`() {
        shadowOf(context.getSystemService(ActivityManager::class.java)).setIsLowRamDevice(true)

        assertEquals(GlassQuality.Frosted, tier()())
    }

    @Test
    fun `a GPU that fell behind on an earlier launch starts the app frosted`() {
        GlassVerdictStore.from(context).recordGpuFallsBehind()

        assertEquals(GlassQuality.Frosted, tier()())
    }

    /** Composes [rememberGlassQuality] and returns a reader of its latest answer. */
    private fun tier(reduceTransparency: Boolean? = null): () -> GlassQuality {
        var latest: GlassQuality? = null
        rule.setContent { latest = rememberGlassQuality(reduceTransparency) }
        return {
            rule.waitForIdle()
            checkNotNull(latest)
        }
    }

    private fun setPowerSave(on: Boolean) {
        power.setIsPowerSaveMode(on)
        context.sendBroadcast(Intent(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED))
    }
}
