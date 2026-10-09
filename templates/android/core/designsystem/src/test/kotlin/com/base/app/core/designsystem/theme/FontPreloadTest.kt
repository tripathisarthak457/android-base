package com.base.app.core.designsystem.theme

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.font.FontFamily
import com.base.app.core.designsystem.TEST_SDK
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TEST_SDK])
class FontPreloadTest {

    @get:Rule
    val rule = createComposeRule()

    private val downloaded = AppFonts(sans = FontFamily.Serif, mono = FontFamily.Cursive)

    @Test
    fun `fonts that arrive in time are used`() = runTest {
        val preload = FontPreload(downloaded)
        assertTrue(preload.run(150.milliseconds) {})
        assertSame(downloaded, preload.fonts)
    }

    @Test
    fun `fonts that never arrive leave the platform's, and nothing recomposes when they turn up later`() {
        val arrival = CompletableDeferred<Unit>()
        val preload = FontPreload(downloaded)
        var arrivedInTime = true
        runTest { arrivedInTime = preload.run(150.milliseconds) { arrival.await() } }
        assertFalse(arrivedInTime)
        assertSame(PlatformFonts, preload.fonts)

        var compositions = 0
        rule.setContent {
            AppTheme(fonts = preload.fonts) {
                SideEffect { compositions++ }
                BasicText("Hello")
            }
        }
        rule.waitForIdle()
        val before = compositions
        arrival.complete(Unit)
        rule.waitForIdle()
        assertEquals(before, compositions)
        assertSame(PlatformFonts, preload.fonts)
    }
}
