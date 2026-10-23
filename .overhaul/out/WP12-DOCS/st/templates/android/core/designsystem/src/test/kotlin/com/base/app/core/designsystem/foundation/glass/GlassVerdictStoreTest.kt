package com.base.app.core.designsystem.foundation.glass

import android.content.Context
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [RobolectricSdk])
class GlassVerdictStoreTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private val preferences =
        context.getSharedPreferences("glass_verdict_test", Context.MODE_PRIVATE)

    @Test
    fun `a device with no verdict is assumed to keep up`() {
        assertTrue(GlassVerdictStore(preferences, build = "phone/1@100").gpuKeepsUp)
    }

    @Test
    fun `the verdict outlives the process for the same build and install`() {
        GlassVerdictStore(preferences, build = "phone/1@100").recordGpuFallsBehind()

        assertFalse(GlassVerdictStore(preferences, build = "phone/1@100").gpuKeepsUp)
    }

    @Test
    fun `a system update or a new install measures again`() {
        GlassVerdictStore(preferences, build = "phone/1@100").recordGpuFallsBehind()

        assertTrue(GlassVerdictStore(preferences, build = "phone/2@100").gpuKeepsUp)
        assertTrue(GlassVerdictStore(preferences, build = "phone/1@200").gpuKeepsUp)
    }

    @Test
    fun `the store reads its own build from the running app`() {
        GlassVerdictStore.from(context).recordGpuFallsBehind()

        assertFalse(GlassVerdictStore.from(context).gpuKeepsUp)
    }
}
