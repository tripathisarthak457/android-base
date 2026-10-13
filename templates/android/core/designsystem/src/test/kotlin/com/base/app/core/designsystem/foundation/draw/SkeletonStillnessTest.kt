package com.base.app.core.designsystem.foundation.draw

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import com.base.app.core.designsystem.TestSdk
import com.base.app.core.designsystem.component.feedback.AppSkeleton
import com.base.app.core.designsystem.foundation.LocalTabVisible
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.LocalAppColors
import com.base.app.core.designsystem.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Where the shimmer must not move, it does not: with motion reduced, in a hidden tab, in a preview,
 * and outside the theme, a bone is its plain colour on every frame and the clock never runs.
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
            CompositionLocalProvider(LocalTabVisible provides false) { content() }
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
