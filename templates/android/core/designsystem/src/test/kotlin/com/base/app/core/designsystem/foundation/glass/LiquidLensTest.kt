package com.base.app.core.designsystem.foundation.glass

import android.provider.Settings
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
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
    fun `a lens at rest neither samples nor draws`() {
        val backdrop = CountingBackdrop(Color.Blue)
        rule.setContent {
            CompositionLocalProvider(LocalGlassQuality provides GlassQuality.Live) {
                Lens(backdrop, rememberLens(selected = 0))
            }
        }
        rule.mainClock.autoAdvance = false

        val atRest = lensCentre()

        assertEquals(Background, atRest)
        assertEquals(0, backdrop.samples)
    }

    @Test
    fun `a pressed lens shows its backdrop`() {
        val backdrop = CountingBackdrop(Color.Blue)
        lateinit var state: LiquidLensState
        rule.setContent {
            CompositionLocalProvider(LocalGlassQuality provides GlassQuality.Live) {
                state = rememberLens(selected = 0)
                Lens(backdrop, state)
            }
        }
        rule.mainClock.autoAdvance = false

        state.press(0f)
        framesUntil { state.pressure == 1f }
        val pressed = lensCentre()

        assertTrue("the pressed lens is glass over its backdrop: $pressed", pressed.isBlue())
        assertTrue(backdrop.samples > 0)
    }

    @Test
    fun `a pressed lens swells past its resting bounds along and across the bar`() {
        val backdrop = CountingBackdrop(Color.Blue)
        lateinit var state: LiquidLensState
        rule.setContent {
            CompositionLocalProvider(LocalGlassQuality provides GlassQuality.Live) {
                state = rememberLens(selected = 0)
                Lens(backdrop, state)
            }
        }
        rule.mainClock.autoAdvance = false

        state.press(0f)
        framesUntil { state.pressure == 1f }
        val (along, across) = besideLens(Orientation.Horizontal)

        assertTrue("along the bar: $along", along.isBlue())
        assertTrue("across the bar: $across", across.isBlue())
    }

    @Test
    fun `a selection's lens is glass on screen while it travels`() {
        val backdrop = CountingBackdrop(Color.Blue)
        var selected by mutableIntStateOf(0)
        lateinit var state: LiquidLensState
        rule.setContent {
            CompositionLocalProvider(LocalGlassQuality provides GlassQuality.Live) {
                state = rememberLens(selected)
                Lens(backdrop, state)
            }
        }
        rule.mainClock.autoAdvance = false

        selected = 2
        framesUntil { state.travel > 0.95f }
        val inTransit = lensCentre()

        // A layer records its content whatever its alpha, so only the pixels show the lens is seen.
        assertTrue("the travelling lens is glass over its backdrop: $inTransit", inTransit.isBlue())
        assertTrue(backdrop.samples > 0)
    }

    @Test
    fun `a settled lens drawn again samples nothing`() {
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
        lensCentre()
        framesUntil { state.position == 2f && state.velocity == 0f }
        backdrop.samples = 0
        val drawsWhenSettled = surfaceDraws

        surface = Color.Yellow
        val settled = lensCentre()

        assertTrue("the settled lens is drawn again", surfaceDraws > drawsWhenSettled)
        assertEquals("without sampling", 0, backdrop.samples)
        assertEquals(Background, settled)
    }

    @Test
    fun `a travelling lens stretches along the bar`() {
        val backdrop = CountingBackdrop(Color.Blue)
        var selected by mutableIntStateOf(0)
        lateinit var state: LiquidLensState
        rule.setContent {
            CompositionLocalProvider(LocalGlassQuality provides GlassQuality.Live) {
                state = rememberLens(selected)
                Lens(backdrop, state)
            }
        }
        rule.mainClock.autoAdvance = false

        selected = 2
        framesUntil { state.travel > 0.95f }
        val (along, across) = besideLens(Orientation.Horizontal)

        assertTrue("along the bar the lens reaches past its bounds: $along", along.isBlue())
        assertEquals("across the bar it stays within a sliver of them", Background, across)
    }

    @Test
    fun `a lens that has settled is back within its resting bounds`() {
        val backdrop = CountingBackdrop(Color.Blue)
        var selected by mutableIntStateOf(0)
        lateinit var state: LiquidLensState
        rule.setContent {
            CompositionLocalProvider(LocalGlassQuality provides GlassQuality.Live) {
                state = rememberLens(selected)
                Lens(backdrop, state)
            }
        }
        rule.mainClock.autoAdvance = false
        selected = 2
        framesUntil { state.travel > 0.95f }
        besideLens(Orientation.Horizontal)

        framesUntil { state.position == 2f && state.velocity == 0f }
        val (along, across) = besideLens(Orientation.Horizontal)

        assertEquals(Background, along)
        assertEquals(Background, across)
    }

    @Test
    fun `a travelling lens on a rail stretches along the rail`() {
        val backdrop = CountingBackdrop(Color.Blue)
        var selected by mutableIntStateOf(0)
        lateinit var state: LiquidLensState
        rule.setContent {
            CompositionLocalProvider(LocalGlassQuality provides GlassQuality.Live) {
                state = rememberLens(selected)
                Lens(backdrop, state, Orientation.Vertical)
            }
        }
        rule.mainClock.autoAdvance = false

        selected = 2
        framesUntil { state.travel > 0.95f }
        val (along, across) = besideLens(Orientation.Vertical)

        assertTrue("along the rail the lens reaches past its bounds: $along", along.isBlue())
        assertEquals("across the rail it stays within a sliver of them", Background, across)
    }

    @Test
    fun `a travelling lens magnifies what is under it`() {
        var selected by mutableIntStateOf(0)
        lateinit var state: LiquidLensState
        rule.setContent {
            CompositionLocalProvider(LocalGlassQuality provides GlassQuality.Live) {
                state = rememberLens(selected)
                Lens(TwoToneBackdrop, state) { Color.Transparent }
            }
        }
        rule.mainClock.autoAdvance = false
        state.press(0f)
        val unmagnified = toneEdgeInLens(state)
        state.cancel()

        selected = 2
        framesUntil { state.travel > 0.95f }
        val inTransit = toneEdgeInLens(state)

        assertEquals("a lens not yet glass shows the edge where it is", 1f, unmagnified, 0.03f)
        assertTrue("a travelling one magnifies it about its centre: $inTransit", inTransit > 1.06f)
    }

    @Test
    fun `a lens that has handed over is not redrawn while it settles`() {
        val backdrop = CountingBackdrop(Color.Blue)
        var selected by mutableIntStateOf(0)
        var surfaceDraws = 0
        lateinit var state: LiquidLensState
        rule.setContent {
            CompositionLocalProvider(LocalGlassQuality provides GlassQuality.Live) {
                state = rememberLens(selected)
                Lens(backdrop, state) {
                    surfaceDraws++
                    Color.Red
                }
            }
        }
        rule.mainClock.autoAdvance = false
        selected = 2
        framesUntil { state.samplesBackdrop }
        framesUntil { !state.samplesBackdrop }
        lensCentre()
        val drawsAtHandOver = surfaceDraws

        val settling = framesUntil {
            lensCentre()
            state.position == 2f && state.velocity == 0f
        }

        assertTrue("the lens still settles for a while: $settling frames", settling > 3)
        assertEquals(drawsAtHandOver, surfaceDraws)
    }

    @Test
    fun `a composition before the app takes up a released tab leaves the lens going there`() {
        var selected by mutableIntStateOf(0)
        var highlighted by mutableStateOf(false)
        var compositions = 0
        lateinit var state: LiquidLensState
        rule.setContent {
            state = rememberLens(selected)
            Box(
                Modifier
                    .size(lensSize(Orientation.Horizontal))
                    .background(if (highlighted) Color.Red else Background),
            )
            SideEffect { compositions++ }
        }
        rule.mainClock.autoAdvance = false
        state.press(0f)
        state.dragBy(2f)
        framesUntil { state.position == 2f && state.velocity == 0f }
        val released = state.release()
        val beforeRecomposing = compositions

        highlighted = true
        repeat(SettleFrames) { frame() }

        assertTrue("the bar recomposed", compositions > beforeRecomposing)
        assertEquals(2, released)
        assertEquals(2f, state.position)
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
        repeat(MaxFrames) { index ->
            frame()
            if (done()) return index + 1
        }
        error("Not done after $MaxFrames frames.")
    }

    private fun frame() {
        Snapshot.sendApplyNotifications()
        rule.mainClock.advanceTimeByFrame()
    }

    /**
     * Robolectric shows a node with a render effect inside an offscreen layer, which Kyant's lens
     * is, one draw late: the first capture after a change still holds the frame before it.
     */
    private fun capture(tag: String): PixelMap {
        Snapshot.sendApplyNotifications()
        val node = rule.onNodeWithTag(tag)
        node.captureToImage()
        return node.captureToImage().toPixelMap()
    }

    private fun lensCentre(): Color {
        val pixels = capture(LensTag)
        return pixels[pixels.width / 2, pixels.height / 2]
    }

    /**
     * The frame's pixels [OutsideGap] beyond the resting bounds of a lens laid along
     * [orientation]: past the middle of its trailing end, along it, and past the middle of its
     * leading side, across it.
     */
    private fun besideLens(orientation: Orientation): Pair<Color, Color> {
        val pixels = capture(FrameTag)
        with(rule.density) {
            val margin = FrameMargin.roundToPx()
            val gap = OutsideGap.roundToPx()
            val length = LensLength.roundToPx()
            val breadth = LensBreadth.roundToPx()
            return when (orientation) {
                Orientation.Horizontal ->
                    pixels[margin + length + gap, margin + breadth / 2] to
                        pixels[margin + length / 2, margin - gap]
                Orientation.Vertical ->
                    pixels[margin + breadth / 2, margin + length + gap] to
                        pixels[margin - gap, margin + length / 2]
            }
        }
    }

    /**
     * Where [TwoToneBackdrop]'s tones meet along the lens's middle row, measured from its centre in
     * the lens's own coordinates (the layer's stretch taken back out), as a share of where they
     * meet in the backdrop itself: 1 for a sample drawn as it is, more for a magnified one.
     */
    private fun toneEdgeInLens(state: LiquidLensState): Float {
        val pixels = capture(LensTag)
        val centre = pixels.width / 2f
        val onScreen = centre - pixels.toneEdge()
        val inLens = onScreen / state.scale(Orientation.Horizontal).scaleX
        return inLens / (centre - pixels.width * ToneSplit)
    }

    /**
     * Where red, which yellow has and blue does not, crosses one half on the way from the middle
     * of the centre row to its leading end, interpolated between pixel centres.
     */
    private fun PixelMap.toneEdge(): Float {
        val row = height / 2
        for (x in width / 2 downTo 1) {
            val inner = this[x, row].red
            val outer = this[x - 1, row].red
            if (inner >= 0.5f && outer < 0.5f) {
                return x + 0.5f - (inner - 0.5f) / (inner - outer)
            }
        }
        error("The backdrop's tones never meet along the lens's middle row.")
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
        orientation: Orientation = Orientation.Horizontal,
        surface: ColorProducer = ColorProducer { Color.Red },
    ) {
        val lens = remember(backdrop, state, orientation, surface) {
            Modifier.liquidLens(backdrop, Capsule(), state, orientation, surface)
        }
        Box(Modifier.testTag(FrameTag).background(Background).padding(FrameMargin)) {
            Box(Modifier.size(lensSize(orientation)).testTag(LensTag).then(lens))
        }
    }

    private fun lensSize(orientation: Orientation): DpSize = when (orientation) {
        Orientation.Horizontal -> DpSize(LensLength, LensBreadth)
        Orientation.Vertical -> DpSize(LensBreadth, LensLength)
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

    /** Blue on its leading [ToneSplit] of the width, yellow on the rest. */
    private object TwoToneBackdrop : Backdrop {
        override val isCoordinatesDependent: Boolean
            get() = false

        override fun DrawScope.drawBackdrop(
            density: Density,
            coordinates: LayoutCoordinates?,
            layerBlock: (GraphicsLayerScope.() -> Unit)?,
        ) {
            val split = size.width * ToneSplit
            drawRect(Color.Blue, size = Size(split, size.height))
            drawRect(Color.Yellow, topLeft = Offset(split, 0f))
        }
    }

    private companion object {
        const val LensTag = "lens"
        const val FrameTag = "frame"
        const val MaxFrames = 120
        const val SettleFrames = 60
        const val ToneSplit = 0.25f
        val Background = Color.Green
        val LensLength = 120.dp
        val LensBreadth = 48.dp
        val FrameMargin = 24.dp
        val OutsideGap = 4.dp
        val TrackSpring: SpringSpec<Float> = spring(dampingRatio = 0.86f, stiffness = 1755f)
        val SettleSpring: SpringSpec<Float> = spring(dampingRatio = 1f, stiffness = 504f)
        val PressSpring: SpringSpec<Float> = spring(dampingRatio = 1f, stiffness = 1542f)
    }
}
