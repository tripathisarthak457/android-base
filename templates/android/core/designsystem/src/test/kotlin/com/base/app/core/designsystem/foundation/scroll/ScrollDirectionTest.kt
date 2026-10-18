package com.base.app.core.designsystem.foundation.scroll

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
import com.base.app.core.designsystem.TestSdk
import com.base.app.core.designsystem.setThemed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

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

    /** Where the content is in each frame after a flick, the clock held so no frame is skipped. */
    private fun flick(towards: Float, velocity: Float): List<Float> {
        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag("surface").performTouchInput {
            swipeWithVelocity(start = center, end = center + surface.along(towards * FlickPx), endVelocity = velocity)
        }
        return List(SettleFrames) {
            rule.mainClock.advanceTimeByFrame()
            travelled()
        }
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

    private fun flingIntoTheEndCarriesPastIt(surface: Surface) {
        show(surface) { it - NearEndPx }
        val end = scroll.maxValue.toFloat()
        val past = flick(surface.forward, HardFling).map { it - end }
        // It meets the end at nearly 6000 px/s, so the band takes the 5000 px/s cap into bounceBack
        // (k 200, ζ 1.1) and carries the content about 120 px past the end.
        val carried = past.max()
        assertTrue("the fling stopped at the end instead of carrying on past it: $carried px", carried > CarriedPastPx)
        val back = past.dropWhile { it < 0f }.min()
        assertTrue("springing back, the content went past the end into the list: $back px", back >= -0.5f)
        assertEquals("the band did not settle back on the end", 0f, past.last(), 0.5f)
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
        flingIntoTheEndCarriesPastIt(Surface.Column)

    @Test
    fun `row - a fling into the end carries on past it at the speed it got there`() =
        flingIntoTheEndCarriesPastIt(Surface.RowLtr)

    @Test
    fun `right-to-left row - a fling into the end carries on past it at the speed it got there`() =
        flingIntoTheEndCarriesPastIt(Surface.RowRtl)

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

        /** Long enough for the slowest fling here, and a band's spring after it, to come to rest. */
        const val SettleFrames = 260
    }
}
