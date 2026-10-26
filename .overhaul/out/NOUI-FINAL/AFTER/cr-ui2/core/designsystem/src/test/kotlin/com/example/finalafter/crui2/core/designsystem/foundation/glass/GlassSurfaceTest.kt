package com.example.finalafter.crui2.core.designsystem.foundation.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [RobolectricSdk])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GlassSurfaceTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `the shadow falls outside the glass and never shows through its body`() {
        val clear = GlassColors(
            body = Color.Transparent,
            rim = Color.Transparent,
            sheen = Color.Transparent,
        )
        val pixels = capture(clear, shadow = Shadow(radius = 12.dp, alpha = 1f))

        assertEquals("inside the glass", Color.White.toArgb(), pixels.inside)
        assertTrue("just below the glass", pixels.below.luminance() < 0.9f)
    }

    @Test
    fun `the body is drawn under the content, and nothing else is without a shadow`() {
        val solid =
            GlassColors(body = Color.Red, rim = Color.Transparent, sheen = Color.Transparent)
        val pixels = capture(solid, shadow = null)

        assertEquals("inside the glass", Color.Red.toArgb(), pixels.inside)
        assertEquals("just below the glass", Color.White.toArgb(), pixels.below.toArgb())
    }

    private class Pixels(val inside: Int, val below: Color)

    private fun capture(colors: GlassColors, shadow: Shadow?): Pixels {
        rule.setContent {
            Box(
                Modifier.size(FrameSize).background(Color.White).testTag("frame"),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(GlassSize)
                        .glassSurface(RoundedCornerShape(16.dp), colors, shadow),
                )
            }
        }
        val image = rule.onNodeWithTag("frame").captureToImage().toPixelMap()
        val centre = image.width / 2
        val glassBottom = with(rule.density) { ((FrameSize + GlassSize) / 2).roundToPx() }
        return Pixels(
            inside = image[centre, centre].toArgb(),
            below = image[centre, glassBottom + with(rule.density) { 4.dp.roundToPx() }],
        )
    }

    private companion object {
        val FrameSize = 200.dp
        val GlassSize = 100.dp
    }
}
