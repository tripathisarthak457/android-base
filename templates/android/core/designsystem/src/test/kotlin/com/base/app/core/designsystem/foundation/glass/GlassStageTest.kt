package com.base.app.core.designsystem.foundation.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.drawPlainBackdrop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
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
class GlassStageTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `chrome samples the part of the scene that lies under it`() {
        rule.setContent {
            Stage(chrome = { Sampler(Alignment.TopCenter, "top") }) { Halves() }
        }

        assertTrue(rule.onNodeWithTag("top").every(Color.Red))
    }

    @Test
    fun `a sample follows the chrome to the other end of the scene`() {
        rule.setContent {
            Stage(chrome = { Sampler(Alignment.BottomCenter, "bottom") }) { Halves() }
        }

        assertTrue(rule.onNodeWithTag("bottom").every(Color.Blue))
    }

    @Test
    fun `where the scene is transparent a sample shows the window background`() {
        rule.setContent {
            Stage(background = Color.Green, chrome = { Sampler(Alignment.TopCenter, "top") }) {}
        }

        assertTrue(rule.onNodeWithTag("top").every(Color.Green))
    }

    @Test
    fun `live chrome tints the scene behind it, frosted chrome all but hides it`() {
        var quality by mutableStateOf(GlassQuality.Live)
        rule.setContent {
            Stage(
                quality = quality,
                chrome = {
                    Box(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .size(SamplerWidth, SamplerHeight)
                            .testTag("chrome")
                            .then(rememberGlassChrome(RectangleShape)),
                    )
                },
            ) {
                Halves()
            }
        }
        val live = rule.onNodeWithTag("chrome").centre()
        quality = GlassQuality.Frosted
        val frosted = rule.onNodeWithTag("chrome").centre()

        assertTrue("the blue scene shows through live glass: $live", live.blue > live.red + 0.2f)
        assertTrue("and is tinted: $live", live.red > 0.1f)
        assertTrue("frosted glass is nearly opaque: $frosted", frosted.luminance() > 0.8f)
    }

    @Test
    fun `tiers that sample nothing give chrome no scene`() {
        val scenes = mutableMapOf<GlassQuality, SceneBackdrop?>()
        var quality by mutableStateOf(GlassQuality.Live)
        rule.setContent {
            Stage(quality = quality, chrome = { scenes[quality] = LocalGlassScene.current }) {}
        }
        for (tier in GlassQuality.entries) {
            quality = tier
            rule.waitForIdle()
        }

        assertNotNull(scenes[GlassQuality.Live])
        assertNotNull(scenes[GlassQuality.LiveNoLens])
        assertNull(scenes[GlassQuality.Frosted])
        assertNull(scenes[GlassQuality.Solid])
    }

    @Test
    fun `a tier change keeps the content's composition and state`() {
        var created = 0
        var quality by mutableStateOf(GlassQuality.Live)
        rule.setContent {
            Stage(quality = quality, chrome = {}) {
                remember { created++ }
            }
        }
        for (tier in GlassQuality.entries + GlassQuality.Live) {
            quality = tier
            rule.waitForIdle()
        }

        assertEquals(1, created)
    }

    @Test
    fun `a surface inside the scene it samples fails with a reason, not a render thread crash`() {
        lateinit var scene: SceneBackdrop
        lateinit var inside: LayoutCoordinates
        rule.setContent {
            val layer = rememberGraphicsLayer()
            scene = remember(layer) { SceneBackdrop(layer) { Color.White } }
            Box(Modifier.size(StageSize).recordScene(scene)) {
                Box(Modifier.size(SamplerWidth, SamplerHeight).onGloballyPositioned { inside = it })
            }
        }

        // Sampled directly rather than through a frame: the test rule reports an exception thrown
        // in a draw pass again at teardown, after the assertion has already caught it.
        val failure = rule.runOnIdle {
            assertThrows(IllegalStateException::class.java) { scene.sampleFrom(inside) }
        }

        assertTrue(failure.message.orEmpty().contains("chrome slot"))
    }

    @Composable
    private fun Stage(
        chrome: @Composable BoxScope.() -> Unit,
        background: Color = Color.White,
        quality: GlassQuality = GlassQuality.Live,
        content: @Composable () -> Unit,
    ) {
        GlassStage(
            background = background,
            chrome = chrome,
            modifier = Modifier.size(StageSize),
            quality = quality,
            content = content,
        )
    }

    @Composable
    private fun BoxScope.Sampler(alignment: Alignment, tag: String) {
        val scene = LocalGlassScene.current
        Box(
            Modifier
                .align(alignment)
                .size(SamplerWidth, SamplerHeight)
                .testTag(tag)
                .then(
                    if (scene != null) {
                        Modifier.drawPlainBackdrop(scene, { RectangleShape }, effects = {})
                    } else {
                        Modifier
                    },
                ),
        )
    }

    /** The scene: red above, blue below. */
    @Composable
    private fun Halves() {
        Column(Modifier.size(StageSize)) {
            Box(Modifier.fillMaxWidth().height(StageSize / 2).background(Color.Red))
            Box(Modifier.fillMaxWidth().height(StageSize / 2).background(Color.Blue))
        }
    }

    private fun SceneBackdrop.sampleFrom(surface: LayoutCoordinates) {
        val size = Size(1f, 1f)
        CanvasDrawScope().draw(rule.density, LayoutDirection.Ltr, Canvas(ImageBitmap(1, 1)), size) {
            drawBackdrop(rule.density, surface, layerBlock = null)
        }
    }

    private fun SemanticsNodeInteraction.centre(): Color {
        val pixels = captureToImage().toPixelMap()
        return pixels[pixels.width / 2, pixels.height / 2]
    }

    private fun SemanticsNodeInteraction.every(color: Color): Boolean {
        val pixels = captureToImage().toPixelMap()
        val argb = color.toArgb()
        return (0 until pixels.width).all { x ->
            (0 until pixels.height).all { y -> pixels[x, y].toArgb() == argb }
        }
    }

    private companion object {
        val StageSize = 200.dp
        val SamplerWidth = 120.dp
        val SamplerHeight = 40.dp
    }
}
