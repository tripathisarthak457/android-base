package com.base.app.core.designsystem.foundation.glass

import android.provider.Settings
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.shapes.Capsule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [RobolectricSdk])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LiquidLensTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `at Live a selection carries the lens as glass`() {
        val travel = travelOfSelections(GlassQuality.Live)

        assertTrue("$travel", travel.max() > 0.95f)
    }

    @Test
    fun `below Live a selection moves the lens without refracting`() {
        val travel = travelOfSelections(
            GlassQuality.LiveNoLens,
            GlassQuality.Frosted,
            GlassQuality.Solid,
        )

        assertTrue("$travel", travel.all { it == 0f })
    }

    @Test
    fun `with animations off neither a selection nor a drag turns the lens to glass`() {
        val resolver = RuntimeEnvironment.getApplication().contentResolver
        Settings.Global.putFloat(resolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 0f)
        var selected by mutableIntStateOf(0)
        lateinit var state: LiquidLensState
        rule.setContent {
            CompositionLocalProvider(LocalGlassQuality provides GlassQuality.Live) {
                state = rememberLens(selected)
            }
        }
        rule.mainClock.autoAdvance = false

        val glass = mutableListOf<Boolean>()
        selected = 2
        framesUntil {
            glass += state.isGlass()
            state.position == 2f && state.velocity == 0f
        }
        state.press(2f)
        state.dragBy(-2f)
        framesUntil {
            glass += state.isGlass()
            state.position == 0f && state.velocity == 0f
        }

        assertTrue("$glass", glass.none { it })
    }

    @Test
    fun `a travelling lens recomposes nothing`() {
        val backdrop = CountingBackdrop(Color.Blue)
        var selected by mutableIntStateOf(0)
        var compositions = 0
        lateinit var state: LiquidLensState
        rule.setContent {
            CompositionLocalProvider(LocalGlassQuality provides GlassQuality.Live) {
                state = rememberLens(selected)
                Lens(backdrop, state)
                SideEffect { compositions++ }
            }
        }
        rule.mainClock.autoAdvance = false
        val atRest = compositions

        selected = 2
        framesUntil { state.position == 2f && state.velocity == 0f }

        assertEquals("only the selection itself recomposes the bar", atRest + 1, compositions)
    }

    @Test
    fun `a lens at rest neither samples nor draws, and a pressed one shows its backdrop`() {
        val backdrop = CountingBackdrop(Color.Blue)
        lateinit var state: LiquidLensState
        rule.setContent {
            CompositionLocalProvider(LocalGlassQuality provides GlassQuality.Live) {
                state = rememberLens(selected = 0)
                Lens(backdrop, state)
            }
        }
        rule.mainClock.autoAdvance = false
        val atRest = lensCentre()
        val samplesAtRest = backdrop.samples

        state.press(0f)
        framesUntil { state.pressure == 1f }
        val pressed = lensCentre()

        assertEquals(Background, atRest)
        assertEquals(0, samplesAtRest)
        assertTrue("the pressed lens is glass over its backdrop: $pressed", pressed.isBlue())
        assertTrue(backdrop.samples > 0)
    }

    @Test
    fun `a selection carries the lens as glass, and a settled lens samples nothing`() {
        val backdrop = CountingBackdrop(Color.Blue)
        var selected by mutableIntStateOf(0)
        var surface by mutableStateOf(Color.Red)
        var surfaceDraws = 0
        lateinit var state: LiquidLensState
        rule.setContent {
            CompositionLocalProvider(LocalGlassQuality provides GlassQuality.Live) {
                state = rememberLens(selected)
                Lens(backdrop, state) {
                    surfaceDraws++
                    surface
                }
            }
        }
        rule.mainClock.autoAdvance = false

        selected = 2
        framesUntil { state.travel > 0.95f }
        val inTransit = lensCentre()
        val samplesInTransit = backdrop.samples
        framesUntil { state.position == 2f && state.velocity == 0f }
        backdrop.samples = 0
        val drawsWhenSettled = surfaceDraws
        surface = Color.Yellow
        val settled = lensCentre()

        // A layer records its content whatever its alpha, so only the pixels show the lens is seen.
        assertTrue("the travelling lens is glass over its backdrop: $inTransit", inTransit.isBlue())
        assertTrue(samplesInTransit > 0)
        assertTrue("the settled lens is drawn again", surfaceDraws > drawsWhenSettled)
        assertEquals("without sampling", 0, backdrop.samples)
        assertEquals(Background, settled)
    }

    @Test
    fun `a new motion style reaches the lens already in the bar`() {
        var settleSpring by mutableStateOf(SettleSpring)
        var selected by mutableIntStateOf(0)
        lateinit var state: LiquidLensState
        rule.setContent {
            state = rememberLens(selected, settleSpring)
        }
        rule.mainClock.autoAdvance = false
        val created = state

        selected = 2
        val onStyle = framesUntil { state.position == 2f && state.velocity == 0f }
        settleSpring = spring(dampingRatio = 1f, stiffness = 2000f)
        selected = 0
        val onNewStyle = framesUntil { state.position == 0f && state.velocity == 0f }

        assertSame("the bar keeps its lens", created, state)
        assertTrue("$onNewStyle frames against $onStyle", onNewStyle < onStyle)
    }

    /** The lens's travel on every frame of a selection made at each of [tiers] in turn. */
    private fun travelOfSelections(vararg tiers: GlassQuality): List<Float> {
        var quality by mutableStateOf(tiers.first())
        var selected by mutableIntStateOf(0)
        lateinit var state: LiquidLensState
        rule.setContent {
            CompositionLocalProvider(LocalGlassQuality provides quality) {
                state = rememberLens(selected)
            }
        }
        rule.mainClock.autoAdvance = false

        val travel = mutableListOf<Float>()
        for (tier in tiers) {
            quality = tier
            selected = 2 - selected
            framesUntil {
                travel += state.travel
                state.position == selected.toFloat() && state.velocity == 0f
            }
        }
        return travel
    }

    /**
     * Runs frames until [done]. The state writes this test makes from outside a composition reach
     * the composition and the lens's snapshotFlow only through apply notifications, which nothing
     * sends while the clock is driven by hand, so each frame sends them first, as Android's frame
     * does. Returns how many frames that took.
     */
    private fun framesUntil(done: () -> Boolean): Int {
        repeat(MaxFrames) { frame ->
            Snapshot.sendApplyNotifications()
            rule.mainClock.advanceTimeByFrame()
            if (done()) return frame + 1
        }
        error("Not done after $MaxFrames frames.")
    }

    /**
     * Robolectric shows a node with a render effect inside an offscreen layer, which Kyant's lens
     * is, one draw late: the first capture after a change still holds the frame before it.
     */
    private fun lensCentre(): Color {
        val lens = rule.onNodeWithTag(LensTag)
        lens.captureToImage()
        val pixels = lens.captureToImage().toPixelMap()
        return pixels[pixels.width / 2, pixels.height / 2]
    }

    private fun LiquidLensState.isGlass() = samplesBackdrop || intensity > 0f

    private fun Color.isBlue() = blue > 0.8f && red < 0.2f && green < 0.2f

    @Composable
    private fun rememberLens(
        selected: Int,
        settleSpring: SpringSpec<Float> = SettleSpring,
    ): LiquidLensState = rememberLiquidLensState(
        selectedSlot = selected,
        slotCount = 3,
        trackSpring = TrackSpring,
        settleSpring = settleSpring,
        pressSpring = PressSpring,
    )

    @Composable
    private fun Lens(
        backdrop: Backdrop,
        state: LiquidLensState,
        surface: ColorProducer = ColorProducer { Color.Red },
    ) {
        val lens = remember(backdrop, state, surface) {
            Modifier.liquidLens(backdrop, Capsule(), state, Orientation.Horizontal, surface)
        }
        Box(Modifier.size(LensWidth, LensHeight).background(Background)) {
            Box(Modifier.size(LensWidth, LensHeight).testTag(LensTag).then(lens))
        }
    }

    /** A backdrop of one colour that counts how often a lens samples it. */
    private class CountingBackdrop(private val color: Color) : Backdrop {
        var samples = 0

        override val isCoordinatesDependent: Boolean
            get() = false

        override fun DrawScope.drawBackdrop(
            density: Density,
            coordinates: LayoutCoordinates?,
            layerBlock: (GraphicsLayerScope.() -> Unit)?,
        ) {
            samples++
            drawRect(color)
        }
    }

    private companion object {
        const val LensTag = "lens"
        const val MaxFrames = 120
        val Background = Color.Green
        val LensWidth = 120.dp
        val LensHeight = 48.dp
        val TrackSpring: SpringSpec<Float> = spring(dampingRatio = 0.86f, stiffness = 1755f)
        val SettleSpring: SpringSpec<Float> = spring(dampingRatio = 1f, stiffness = 504f)
        val PressSpring: SpringSpec<Float> = spring(dampingRatio = 1f, stiffness = 1542f)
    }
}
