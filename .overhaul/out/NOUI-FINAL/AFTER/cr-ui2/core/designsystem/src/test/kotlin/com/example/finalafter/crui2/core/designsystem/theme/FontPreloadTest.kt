package com.example.finalafter.crui2.core.designsystem.theme

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.font.FontFamily
import com.example.finalafter.crui2.core.designsystem.TestSdk
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
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
@Config(sdk = [TestSdk])
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
    fun `a device with no font provider launches with the platform's`() = runTest {
        val preload = FontPreload(downloaded)
        assertFalse(preload.run(150.milliseconds) { throw IllegalStateException("Unable to load font") })
        assertSame(PlatformFonts, preload.fonts)
    }

    @Test
    fun `the budget running out inside the resolver leaves the platform's`() = runTest {
        val preload = FontPreload(downloaded)
        assertFalse(preload.run(150.milliseconds) { resolverThatWrapsCancellation() })
        assertSame(PlatformFonts, preload.fonts)
    }

    @Test
    fun `cancelling the launch cancels the preload rather than settling on the platform's`() = runTest {
        val preload = FontPreload(downloaded)
        var returned = false
        val startup = launch {
            preload.run(10.seconds) { resolverThatWrapsCancellation() }
            returned = true
        }
        runCurrent()
        startup.cancel()
        startup.join()
        assertTrue(startup.isCancelled)
        assertFalse(returned)
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

    /**
     * What Compose's FontFamily.Resolver.preload does with a font still loading when its caller is
     * cancelled: it catches the cancellation and throws IllegalStateException in its place.
     */
    private suspend fun resolverThatWrapsCancellation() {
        try {
            awaitCancellation()
        } catch (cancelled: CancellationException) {
            throw IllegalStateException("Unable to load font", cancelled)
        }
    }
}
