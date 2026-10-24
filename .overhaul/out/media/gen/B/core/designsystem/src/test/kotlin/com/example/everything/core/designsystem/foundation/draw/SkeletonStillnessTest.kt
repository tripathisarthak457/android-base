package com.example.everything.core.designsystem.foundation.draw

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.example.everything.core.designsystem.TestSdk
import com.example.everything.core.designsystem.component.feedback.AppSkeleton
import com.example.everything.core.designsystem.foundation.layout.LocalTabVisible
import com.example.everything.core.designsystem.foundation.layout.TabVisibility
import com.example.everything.core.designsystem.theme.AppTheme
import com.example.everything.core.designsystem.theme.LocalAppColors
import com.example.everything.core.designsystem.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Where the shimmer must not move, it does not: with motion reduced, in a hidden tab, in a preview,
 * and outside the theme, a bone is its plain colour on every frame and the clock never runs; motion
 * reduced while a sweep runs stops it at once. A bone that holds still is never redrawn by the
 * clock, so a theme change has to reach it on its own.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestSdk], qualifiers = "w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SkeletonStillnessTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `with motion reduced a bone holds still`() = assertStill { content ->
        AppTheme(mode = ThemeMode.Light, reduceMotion = true) { content() }
    }

    @Test
    fun `in a hidden tab a bone holds still`() = assertStill { content ->
        AppTheme(mode = ThemeMode.Light, reduceMotion = false) {
            CompositionLocalProvider(LocalTabVisible provides TabVisibility { false }) { content() }
        }
    }

    @Test
    fun `in a preview a bone holds still`() = assertStill { content ->
        CompositionLocalProvider(LocalInspectionMode provides true) {
            AppTheme(mode = ThemeMode.Light, reduceMotion = false) { content() }
        }
    }

    @Test
    fun `outside the theme a bone holds still`() = assertStill { content -> content() }

    @Test
    fun `reducing motion mid-sweep stills the bone, and allowing it again resumes the sweep`() {
        var reduceMotion by mutableStateOf(false)
        var bone = Color.Unspecified
        var clock: ShimmerClock? = null
        rule.mainClock.autoAdvance = false
        rule.setContent {
            AppTheme(mode = ThemeMode.Light, reduceMotion = reduceMotion) {
                bone = LocalAppColors.current.skeleton
                clock = LocalShimmerClock.current
                Box(Modifier.testTag("bone")) {
                    AppSkeleton(Modifier.size(300.dp, 24.dp), shape = RectangleShape)
                }
            }
        }
        assertTrue("the sweep never started", advanceUntilSweeping { clock })

        // Through the theme's override, as the catalog's switch flips it. The setting reaches a node
        // through a static local, which the node does not observe: it sees the change only if the
        // theme keeps one state and changes its value, rather than providing a new one.
        reduceMotion = true
        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame()
        assertTrue("the sweep went on", clock?.progress?.isNaN() ?: false)
        val stilled = rule.onNodeWithTag("bone").captureToImage().toPixelMap()
        repeat(StillFrames) {
            rule.mainClock.advanceTimeByFrame()
            assertTrue("the clock ran with motion reduced", clock?.progress?.isNaN() ?: false)
        }
        val later = rule.onNodeWithTag("bone").captureToImage().toPixelMap()
        val inset = 2
        for (y in inset until stilled.height - inset) {
            for (x in inset until stilled.width - inset) {
                assertEquals("($x, $y)", bone.toArgb(), stilled[x, y].toArgb())
                assertEquals("($x, $y) moved", stilled[x, y].toArgb(), later[x, y].toArgb())
            }
        }

        reduceMotion = false
        rule.waitForIdle()
        assertTrue("the sweep did not resume", advanceUntilSweeping { clock })
    }

    @Test
    fun `with motion reduced a bone takes a new theme's colour on the next frame`() {
        var mode by mutableStateOf(ThemeMode.Light)
        var bone = Color.Unspecified
        rule.mainClock.autoAdvance = false
        rule.setContent {
            AppTheme(mode = mode, reduceMotion = true) {
                bone = LocalAppColors.current.skeleton
                Box(Modifier.testTag("bone")) {
                    AppSkeleton(Modifier.size(300.dp, 24.dp), shape = RectangleShape)
                }
            }
        }
        rule.mainClock.advanceTimeByFrame()
        val light = bone
        // Drawn once in the light theme, so the bone's layer holds a recording a repaint must replace.
        assertEquals(light.toArgb(), rule.onNodeWithTag("bone").captureToImage().toPixelMap()[10, 10].toArgb())

        mode = ThemeMode.Dark
        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame()
        assertNotEquals("the themes share a bone colour", light, bone)

        val pixels = rule.onNodeWithTag("bone").captureToImage().toPixelMap()
        val inset = 2
        for (y in inset until pixels.height - inset) {
            for (x in inset until pixels.width - inset) {
                assertEquals("($x, $y)", bone.toArgb(), pixels[x, y].toArgb())
            }
        }
    }

    /** Advances frame by frame until the clock is mid-sweep; false if no sweep starts in time. */
    private fun advanceUntilSweeping(clock: () -> ShimmerClock?): Boolean {
        repeat(StillFrames) {
            rule.mainClock.advanceTimeByFrame()
            if (clock()?.progress?.isNaN() == false) return true
        }
        return false
    }

    private fun assertStill(host: @Composable (content: @Composable () -> Unit) -> Unit) {
        var bone = Color.Unspecified
        var clock: ShimmerClock? = null
        rule.mainClock.autoAdvance = false
        rule.setContent {
            host {
                bone = LocalAppColors.current.skeleton
                clock = LocalShimmerClock.current
                Column(Modifier.testTag("bones")) {
                    AppSkeleton(Modifier.size(300.dp, 24.dp), shape = RectangleShape)
                    AppSkeleton(Modifier.size(120.dp, 120.dp), shape = RectangleShape)
                }
            }
        }
        rule.mainClock.advanceTimeByFrame()
        val first = rule.onNodeWithTag("bones").captureToImage().toPixelMap()
        repeat(StillFrames) {
            rule.mainClock.advanceTimeByFrame()
            assertTrue("the clock ran", clock?.progress?.isNaN() ?: true)
        }
        val later = rule.onNodeWithTag("bones").captureToImage().toPixelMap()

        val inset = 2
        for (y in inset until 24) {
            for (x in inset until first.width - inset) {
                assertEquals("($x, $y)", bone.toArgb(), first[x, y].toArgb())
                assertEquals("($x, $y) moved", first[x, y].toArgb(), later[x, y].toArgb())
            }
        }
    }

    private companion object {
        /** Longer than a sweep's pause, so a sweep would have begun. */
        const val StillFrames = 120
    }
}
