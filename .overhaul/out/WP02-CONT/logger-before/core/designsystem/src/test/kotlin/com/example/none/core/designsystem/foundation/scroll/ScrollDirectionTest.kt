package com.example.none.core.designsystem.foundation.scroll

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.none.core.designsystem.TestSdk
import com.example.none.core.designsystem.setThemed
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Which way content goes under the finger on each kind of surface the theme's rubber band and the
 * iOS fling serve: a column, and a row in both layout directions, since a sign that is wrong one way
 * round shows as content moving against the finger. Every case reads [travelled], how far the
 * content has gone towards its end with the band included, so one set of expectations fits all three.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestSdk], qualifiers = "w400dp-h800dp")
class ScrollDirectionTest {

    @get:Rule
    val rule = createComposeRule()

    private enum class Surface(val vertical: Boolean, val direction: LayoutDirection) {
        Column(vertical = true, direction = LayoutDirection.Ltr),
        RowLtr(vertical = false, direction = LayoutDirection.Ltr),
        RowRtl(vertical = false, direction = LayoutDirection.Rtl),
        ;

        /** The way a finger moves to scroll towards the end: up, left, or right in a right-to-left row. */
        val forward: Float get() = if (direction == LayoutDirection.Rtl) 1f else -1f

        fun along(distance: Float): Offset = if (vertical) Offset(0f, distance) else Offset(distance, 0f)
    }

    private lateinit var surface: Surface
    private val scroll = ScrollState(0)
    private var viewport: LayoutCoordinates? = null
    private var firstItem: LayoutCoordinates? = null

    private fun show(surface: Surface, startAt: (max: Int) -> Int) {
        this.surface = surface
        rule.setThemed {
            CompositionLocalProvider(LocalLayoutDirection provides surface.direction) {
                val fling = AppScrollDefaults.flingBehavior()
                val box = Modifier.testTag("surface").onGloballyPositioned { viewport = it }.size(ViewportSide)
                if (surface.vertical) {
                    Column(box.verticalScroll(scroll, flingBehavior = fling)) { Items() }
                } else {
                    Row(box.horizontalScroll(scroll, flingBehavior = fling)) { Items() }
                }
            }
        }
        val start = rule.runOnIdle { startAt(scroll.maxValue) }
        rule.runOnIdle { scroll.dispatchRawDelta(start.toFloat()) }
        rule.waitForIdle()
        assertEquals(start.toFloat(), travelled(), 0.5f)
    }

    @Composable
    private fun Items() {
        Box(Modifier.size(ItemSide).onGloballyPositioned { firstItem = it })
        repeat(ItemCount - 1) { Box(Modifier.size(ItemSide)) }
    }

    /**
     * How far the content has moved towards its end, in pixels: the scroll offset, plus the rubber
     * band's stretch past either end (below 0 before the start, beyond the maximum past the end).
     */
    private fun travelled(): Float = rule.runOnUiThread {
        val item = checkNotNull(firstItem)
        val box = checkNotNull(viewport)
        val itemAt = item.positionInRoot()
        val boxAt = box.positionInRoot()
        when {
            surface.vertical -> boxAt.y - itemAt.y
            surface.direction == LayoutDirection.Ltr -> boxAt.x - itemAt.x
            else -> (itemAt.x + item.size.width) - (boxAt.x + box.size.width)
        }
    }

    private fun gesture(block: TouchInjectionScope.() -> Unit) {
        rule.onNodeWithTag("surface").performTouchInput(block)
        rule.waitForIdle()
    }

    /** The finger down in the middle and moved one pixel past the touch slop, [towards] that way. */
    private fun TouchInjectionScope.grab(towards: Float) {
        down(center)
        moveBy(surface.along(towards * (viewConfiguration.touchSlop + 1f)))
    }

    /** Where the content is in each of [frameCount] frames after a flick, the clock held so no frame is skipped. */
    private fun flick(towards: Float, velocity: Float, frameCount: Int = SettleFrames): List<Float> {
        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag("surface").performTouchInput {
            swipeWithVelocity(start = center, end = center + surface.along(towards * FlickPx), endVelocity = velocity)
        }
        return frames(frameCount)
    }

    private fun frames(count: Int): List<Float> = List(count) {
        rule.mainClock.advanceTimeByFrame()
        travelled()
    }

    /**
     * A quick throw [towards] that way, the gesture's last moves. It starts after a pause, so the
     * speed the finger is let go at is the throw's alone.
     */
    private fun TouchInjectionScope.throwBy(towards: Float) {
        repeat(ThrowSteps) { step ->
            val delay = if (step == 0) PauseMillis else ThrowStepMillis
            moveBy(surface.along(towards * ThrowStepPx), delayMillis = delay)
        }
    }

    /** One end of the content, and how far past it the content is: above 0 past it, below 0 inside. */
    private inner class Edge(private val atEnd: Boolean) {
        private val at = if (atEnd) scroll.maxValue.toFloat() else 0f

        /** The way a finger moves to pull the content past this end. */
        val out: Float get() = if (atEnd) surface.forward else -surface.forward

        fun beyond(travelled: Float): Float = if (atEnd) travelled - at else at - travelled
    }

    private fun dragFollowsTheFinger(surface: Surface) {
        show(surface) { it / 2 }
        val start = travelled()
        gesture {
            grab(surface.forward)
            moveBy(surface.along(surface.forward * DragPx))
        }
        val forward = travelled() - start
        assertEquals("a drag towards the end moved the content $forward px", DragPx + 1f, forward, 2f)
        gesture { moveBy(surface.along(-surface.forward * 2 * DragPx)) }
        val back = travelled() - start - forward
        assertEquals("a drag back moved the content $back px", -2 * DragPx, back, 2f)
        gesture { up() }
    }

    /** Past the end, or the start, then back the way it came without lifting the finger. */
    private fun bandFollowsTheFingerBack(surface: Surface, atEnd: Boolean) {
        show(surface) { if (atEnd) it else 0 }
        val edge = travelled()
        val out = if (atEnd) surface.forward else -surface.forward
        gesture {
            grab(out)
            moveBy(surface.along(out * PullPx))
        }
        val stretched = travelled() - edge
        assertTrue("pulling past the edge did not stretch the band: $stretched px", abs(stretched) > 10f)
        assertTrue("the band stretched against the finger: $stretched px", stretched * out * surface.forward > 0f)
        gesture { moveBy(surface.along(-out * PullPx / 2)) }
        val halfway = travelled() - edge
        assertTrue(
            "taken halfway back, the band went from $stretched to $halfway px instead of easing back with the finger",
            abs(halfway) < abs(stretched) && halfway * stretched >= 0f,
        )
        gesture { moveBy(surface.along(-out * (PullPx / 2 + 1f))) }
        assertEquals("back where the drag began, the band is not let go", 0f, travelled() - edge, 1.5f)
        gesture { up() }
    }

    /** A flick [towards] one way from the middle: the content moves only that way, and stops there. */
    private fun flingGoesOnlyTheFingersWay(surface: Surface, towards: Float) {
        show(surface) { it / 2 }
        val start = travelled()
        val path = listOf(start) + flick(towards, MidFling)
        val way = towards * surface.forward
        val steps = path.zipWithNext { a, b -> (b - a) * way }
        val went = path.last() - start
        assertTrue("the fling went $went px, not the finger's way", went * way > FlickPx)
        assertTrue("the content moved against the fling: ${steps.min()} px in one frame", steps.min() >= -0.5f)
        val moving = steps.filter { it > 0.5f }
        assertTrue("the fling did not slow down: $moving", moving.takeLast(5).average() < moving.take(5).average())
        assertTrue("still moving after $SettleFrames frames", abs(path.last() - path[path.size - 2]) < 0.5f)
        assertTrue("came to rest stretched: ${path.last()} px", path.last() in 0f..scroll.maxValue.toFloat())
    }

    private fun flingIntoTheEndCarriesPastIt(surface: Surface, atEnd: Boolean) {
        show(surface) { if (atEnd) it - NearEndPx else NearEndPx }
        val edge = Edge(atEnd)
        val past = flick(edge.out, HardFling).map(edge::beyond)
        val carried = past.max()
        assertTrue("the fling stopped at the end instead of carrying on past it: $carried px", carried > CarriedPastPx)
        val back = past.dropWhile { it < 0f }.min()
        assertTrue("springing back, the content went past the end into the list: $back px", back >= -0.5f)
        assertEquals("the band did not settle back on the end", 0f, past.last(), 0.5f)
    }

    /**
     * A fling into an end, then a finger landing while the band springs back and dragging the content
     * back into view: from the touch until the content rests again, it moves only the finger's way.
     */
    private fun caughtBandFollowsTheFinger(surface: Surface, atEnd: Boolean) {
        show(surface) { if (atEnd) it - NearEndPx else NearEndPx }
        val edge = Edge(atEnd)
        flick(edge.out, HardFling, frameCount = 0)
        val caught = untilSpringingBack(edge)
        val path = mutableListOf(caught)
        fun step(input: TouchInjectionScope.() -> Unit) {
            rule.onNodeWithTag("surface").performTouchInput(input)
            rule.mainClock.advanceTimeByFrame()
            path += edge.beyond(travelled())
        }
        step { down(center) }
        step { moveBy(surface.along(-edge.out * (viewConfiguration.touchSlop + 1f))) }
        repeat(CatchSteps) { step { moveBy(surface.along(-edge.out * CatchStepPx)) } }
        step { up() }
        path += frames(SettleFrames).map(edge::beyond)
        val steps = path.zipWithNext { a, b -> b - a }
        assertTrue(
            "caught $caught px past the end, the content moved against the finger: ${steps.max()} px in one frame",
            steps.max() <= 0.5f,
        )
        assertTrue("the drag did not bring the content back into view: ${path.last()} px", path.last() < -CatchStepPx)
    }

    /** Frames until the band, carried past [edge] by a fling, has turned and come part of the way back. */
    private fun untilSpringingBack(edge: Edge): Float {
        var peak = 0f
        repeat(SettleFrames) {
            rule.mainClock.advanceTimeByFrame()
            val past = edge.beyond(travelled())
            peak = maxOf(peak, past)
            if (peak > CarriedPastPx && past < peak * CaughtAt) return past
        }
        throw AssertionError("the band never turned back: it reached $peak px")
    }

    /**
     * Pulled past an end, then flicked back towards the content before the band has eased: the content
     * goes the flick's way the whole time, never past its place and back, and carries on into the list.
     */
    private fun flickBackCarriesOn(surface: Surface, atEnd: Boolean) {
        show(surface) { if (atEnd) it else 0 }
        val edge = Edge(atEnd)
        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag("surface").performTouchInput {
            grab(edge.out)
            moveBy(surface.along(edge.out * PullPx))
            throwBy(-edge.out)
            up()
        }
        val path = frames(SettleFrames).map(edge::beyond)
        assertTrue("let go only ${path.first()} px past the end: the band had eased", path.first() > StretchedPx)
        val steps = path.zipWithNext { a, b -> b - a }
        assertTrue("the content moved against the flick: ${steps.max()} px in one frame", steps.max() <= 0.5f)
        assertTrue(
            "the flick stopped at the end instead of carrying the content on: ${path.last()} px",
            path.last() < -CarriedPastPx,
        )
        assertTrue("still moving after $SettleFrames frames", abs(path.last() - path[path.size - 2]) < 0.5f)
    }

    /** Pulled past the start, then flicked further out: the band springs back onto the start, never past it. */
    private fun flickOutwardSpringsBack(surface: Surface) {
        show(surface) { 0 }
        val edge = Edge(atEnd = false)
        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag("surface").performTouchInput {
            grab(edge.out)
            moveBy(surface.along(edge.out * PullPx / 2))
            throwBy(edge.out)
            up()
        }
        val path = frames(SettleFrames).map(edge::beyond)
        assertTrue(
            "the band was not thrown on out: let go at ${path.first()} px, ${path.max()} px at most",
            path.max() > path.first() + 0.5f,
        )
        assertTrue("springing back, the content went past the start: ${path.min()} px", path.min() >= -0.5f)
        assertEquals("the band did not settle back on the start", 0f, path.last(), 0.5f)
    }

    @Test
    fun `column - the content follows a drag both ways`() = dragFollowsTheFinger(Surface.Column)

    @Test
    fun `row - the content follows a drag both ways`() = dragFollowsTheFinger(Surface.RowLtr)

    @Test
    fun `right-to-left row - the content follows a drag both ways`() = dragFollowsTheFinger(Surface.RowRtl)

    @Test
    fun `column - pulled past the top and back, the band follows the finger back`() =
        bandFollowsTheFingerBack(Surface.Column, atEnd = false)

    @Test
    fun `column - pulled past the bottom and back, the band follows the finger back`() =
        bandFollowsTheFingerBack(Surface.Column, atEnd = true)

    @Test
    fun `row - pulled past the start and back, the band follows the finger back`() =
        bandFollowsTheFingerBack(Surface.RowLtr, atEnd = false)

    @Test
    fun `row - pulled past the end and back, the band follows the finger back`() =
        bandFollowsTheFingerBack(Surface.RowLtr, atEnd = true)

    @Test
    fun `right-to-left row - pulled past the start and back, the band follows the finger back`() =
        bandFollowsTheFingerBack(Surface.RowRtl, atEnd = false)

    @Test
    fun `right-to-left row - pulled past the end and back, the band follows the finger back`() =
        bandFollowsTheFingerBack(Surface.RowRtl, atEnd = true)

    @Test
    fun `column - a fling up comes to rest without coming back`() =
        flingGoesOnlyTheFingersWay(Surface.Column, towards = -1f)

    @Test
    fun `column - a fling down comes to rest without coming back`() =
        flingGoesOnlyTheFingersWay(Surface.Column, towards = 1f)

    @Test
    fun `row - a fling left comes to rest without coming back`() =
        flingGoesOnlyTheFingersWay(Surface.RowLtr, towards = -1f)

    @Test
    fun `row - a fling right comes to rest without coming back`() =
        flingGoesOnlyTheFingersWay(Surface.RowLtr, towards = 1f)

    @Test
    fun `right-to-left row - a fling left comes to rest without coming back`() =
        flingGoesOnlyTheFingersWay(Surface.RowRtl, towards = -1f)

    @Test
    fun `right-to-left row - a fling right comes to rest without coming back`() =
        flingGoesOnlyTheFingersWay(Surface.RowRtl, towards = 1f)

    @Test
    fun `column - a fling into the bottom carries on past it at the speed it got there`() =
        flingIntoTheEndCarriesPastIt(Surface.Column, atEnd = true)

    @Test
    fun `row - a fling into the end carries on past it at the speed it got there`() =
        flingIntoTheEndCarriesPastIt(Surface.RowLtr, atEnd = true)

    @Test
    fun `right-to-left row - a fling into the end carries on past it at the speed it got there`() =
        flingIntoTheEndCarriesPastIt(Surface.RowRtl, atEnd = true)

    @Test
    fun `column - a fling into the top carries on past it at the speed it got there`() =
        flingIntoTheEndCarriesPastIt(Surface.Column, atEnd = false)

    @Test
    fun `row - a fling into the start carries on past it at the speed it got there`() =
        flingIntoTheEndCarriesPastIt(Surface.RowLtr, atEnd = false)

    @Test
    fun `right-to-left row - a fling into the start carries on past it at the speed it got there`() =
        flingIntoTheEndCarriesPastIt(Surface.RowRtl, atEnd = false)

    @Test
    fun `column - caught springing back from the bottom, the content follows the finger back`() =
        caughtBandFollowsTheFinger(Surface.Column, atEnd = true)

    @Test
    fun `column - caught springing back from the top, the content follows the finger back`() =
        caughtBandFollowsTheFinger(Surface.Column, atEnd = false)

    @Test
    fun `row - caught springing back from the end, the content follows the finger back`() =
        caughtBandFollowsTheFinger(Surface.RowLtr, atEnd = true)

    @Test
    fun `row - caught springing back from the start, the content follows the finger back`() =
        caughtBandFollowsTheFinger(Surface.RowLtr, atEnd = false)

    @Test
    fun `right-to-left row - caught springing back from the end, the content follows the finger back`() =
        caughtBandFollowsTheFinger(Surface.RowRtl, atEnd = true)

    @Test
    fun `right-to-left row - caught springing back from the start, the content follows the finger back`() =
        caughtBandFollowsTheFinger(Surface.RowRtl, atEnd = false)

    @Test
    fun `column - pulled past the top and flicked back, the content carries on up the list`() =
        flickBackCarriesOn(Surface.Column, atEnd = false)

    @Test
    fun `column - pulled past the bottom and flicked back, the content carries on down the list`() =
        flickBackCarriesOn(Surface.Column, atEnd = true)

    @Test
    fun `row - pulled past the start and flicked back, the content carries on along the row`() =
        flickBackCarriesOn(Surface.RowLtr, atEnd = false)

    @Test
    fun `row - pulled past the end and flicked back, the content carries on along the row`() =
        flickBackCarriesOn(Surface.RowLtr, atEnd = true)

    @Test
    fun `right-to-left row - pulled past the start and flicked back, the content carries on along the row`() =
        flickBackCarriesOn(Surface.RowRtl, atEnd = false)

    @Test
    fun `right-to-left row - pulled past the end and flicked back, the content carries on along the row`() =
        flickBackCarriesOn(Surface.RowRtl, atEnd = true)

    @Test
    fun `column - pulled past the top and flicked further, the band springs back onto the top`() =
        flickOutwardSpringsBack(Surface.Column)

    @Test
    fun `row - pulled past the start and flicked further, the band springs back onto the start`() =
        flickOutwardSpringsBack(Surface.RowLtr)

    @Test
    fun `right-to-left row - pulled past the start and flicked further, the band springs back onto the start`() =
        flickOutwardSpringsBack(Surface.RowRtl)

    private companion object {
        val ViewportSide = 400.dp
        val ItemSide = 56.dp
        const val ItemCount = 200

        /** Finger travel after the touch slop, in pixels. */
        const val DragPx = 120f
        const val PullPx = 200f
        const val FlickPx = 150f

        /** Pixels a second: a flick that slows to rest well inside the content. */
        const val MidFling = 2_500f

        /** Pixels a second: a firm flick from [NearEndPx] before the end, which the finger alone stops short of. */
        const val HardFling = 6_000f
        const val NearEndPx = 250
        const val CarriedPastPx = 60f

        /** The band is caught once it has come back to this share of how far it was carried. */
        const val CaughtAt = 0.7f
        const val CatchSteps = 30
        const val CatchStepPx = 10f

        /**
         * A throw of [ThrowSteps] moves at 4000 px/s after a [PauseMillis] pause. Thrown back from [PullPx]
         * past an end, it lets go with the band still more than [StretchedPx] out.
         */
        const val ThrowSteps = 8
        const val ThrowStepPx = 20f
        const val ThrowStepMillis = 5L
        const val PauseMillis = 100L
        const val StretchedPx = 10f

        /** Long enough for the slowest fling here, and a band's spring after it, to come to rest. */
        const val SettleFrames = 260
    }
}
