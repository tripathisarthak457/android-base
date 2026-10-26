package com.example.finalafter.crui3.core.designsystem.component.collections

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.finalafter.crui3.core.designsystem.TestSdk
import com.example.finalafter.crui3.core.designsystem.setThemed
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A swipe takes the carousel one card towards where the finger went, in either layout direction, and
 * the card it lands on comes to rest without travelling past its place and back.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestSdk], qualifiers = "w400dp-h800dp")
class AppCarouselTest {

    @get:Rule
    val rule = createComposeRule()

    private lateinit var pager: PagerState
    private lateinit var direction: LayoutDirection
    private var carousel: LayoutCoordinates? = null
    private val cards = mutableMapOf<Int, LayoutCoordinates>()

    private fun show(direction: LayoutDirection, page: Int) {
        this.direction = direction
        rule.setThemed {
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                pager = rememberPagerState(initialPage = page) { Pages }
                AppCarousel(
                    pageCount = Pages,
                    state = pager,
                    modifier = Modifier.height(180.dp).testTag("carousel").onGloballyPositioned { carousel = it },
                ) { card ->
                    Box(Modifier.fillMaxSize().onGloballyPositioned { cards[card] = it })
                }
            }
        }
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
    }

    /** The way a finger moves for the next card: left, or right when the layout runs right to left. */
    private val next: Float get() = if (direction == LayoutDirection.Rtl) 1f else -1f

    /**
     * How far [page]'s card is from the current card's place, in pixels, positive towards the end
     * (later cards): 0 once it is the card shown.
     */
    private fun fromRest(page: Int): Float = rule.runOnUiThread {
        val card = checkNotNull(cards[page])
        val box = checkNotNull(carousel)
        val offset = (card.positionInRoot().x + card.size.width / 2f) - (box.positionInRoot().x + box.size.width / 2f)
        if (direction == LayoutDirection.Rtl) -offset else offset
    }

    /** Swipes a third of the width [towards] that way and records [page]'s card until the carousel rests. */
    private fun swipe(towards: Float, page: Int): List<Float> {
        rule.onNodeWithTag("carousel").performTouchInput {
            swipeWithVelocity(start = center, end = center + Offset(towards * width / 3f, 0f), endVelocity = Flick)
        }
        return List(SettleFrames) {
            rule.mainClock.advanceTimeByFrame()
            fromRest(page)
        }
    }

    private fun swipesOneCard(direction: LayoutDirection, from: Int, forward: Boolean) {
        show(direction, from)
        val target = if (forward) from + 1 else from - 1
        val before = fromRest(target)
        val path = listOf(before) + swipe(if (forward) next else -next, target)
        // The target starts one card towards the end (next) or the start (previous) and only ever closes that gap.
        val side = if (forward) 1f else -1f
        val closing = path.zipWithNext { a, b -> abs(a) - abs(b) }
        assertEquals("landed on card ${pager.currentPage}", target, pager.currentPage)
        assertEquals("settled on card ${pager.settledPage}", target, pager.settledPage)
        assertTrue("the card started on the wrong side: $before px", before * side > 0f)
        val overshoot = -path.minOf { it * side }
        assertTrue("the card went past its place by $overshoot px", overshoot <= 0.5f)
        assertTrue("the card moved away from its place: ${closing.min()} px in one frame", closing.min() >= -0.5f)
        assertEquals("the card did not come to rest in its place", 0f, path.last(), 0.5f)
    }

    /**
     * The first card pulled away from the start and brought back without lifting the finger, slowly
     * enough that letting go there is not a flick: it follows the finger out and back, and stays.
     */
    private fun pulledPastTheFirstCardAndBack(direction: LayoutDirection) {
        show(direction, page = 0)
        val start = -next
        fun drag(towards: Float, distance: Float): Float {
            rule.onNodeWithTag("carousel").performTouchInput {
                repeat((distance / DragStepPx).toInt()) { moveBy(Offset(towards * DragStepPx, 0f)) }
            }
            rule.mainClock.advanceTimeByFrame()
            return fromRest(0)
        }
        rule.onNodeWithTag("carousel").performTouchInput {
            down(center)
            moveBy(Offset(start * (viewConfiguration.touchSlop + 1f), 0f))
        }
        val stretched = drag(start, PullPx)
        assertTrue("pulled away from the start, the first card did not follow: $stretched px", stretched > 1f)
        val halfway = drag(-start, PullPx / 2)
        assertTrue(
            "taken halfway back, the first card went from $stretched to $halfway px instead of following the finger",
            halfway < stretched && halfway >= -0.5f,
        )
        drag(-start, PullPx / 2)
        rule.onNodeWithTag("carousel").performTouchInput { up() }
        val path = List(SettleFrames) {
            rule.mainClock.advanceTimeByFrame()
            fromRest(0)
        }
        assertEquals("settled on card ${pager.settledPage}", 0, pager.settledPage)
        assertEquals("the first card did not come to rest in its place", 0f, path.last(), 0.5f)
    }

    @Test
    fun `pulled right past the first card and back, it follows the finger back`() =
        pulledPastTheFirstCardAndBack(LayoutDirection.Ltr)

    @Test
    fun `right to left layout, pulled left past the first card and back, it follows the finger back`() =
        pulledPastTheFirstCardAndBack(LayoutDirection.Rtl)

    @Test
    fun `swiped right to left, it moves to the next card`() =
        swipesOneCard(LayoutDirection.Ltr, from = 0, forward = true)

    @Test
    fun `swiped right to left onto the last card, it lands on it`() =
        swipesOneCard(LayoutDirection.Ltr, from = Pages - 2, forward = true)

    @Test
    fun `swiped left to right, it moves to the previous card`() =
        swipesOneCard(LayoutDirection.Ltr, from = 2, forward = false)

    @Test
    fun `right to left layout, swiped left to right, it moves to the next card`() =
        swipesOneCard(LayoutDirection.Rtl, from = 0, forward = true)

    @Test
    fun `right to left layout, swiped left to right onto the last card, it lands on it`() =
        swipesOneCard(LayoutDirection.Rtl, from = Pages - 2, forward = true)

    @Test
    fun `right to left layout, swiped right to left, it moves to the previous card`() =
        swipesOneCard(LayoutDirection.Rtl, from = 2, forward = false)

    private companion object {
        const val Pages = 5

        /** Pixels a second: a quick flick, well past the paging threshold. */
        const val Flick = 2_000f

        /**
         * A slow pull: one step an event, 250 px/s, under the pager's 400 dp/s flick so the release
         * at the start is a let-go.
         */
        const val PullPx = 120f
        const val DragStepPx = 4f
        const val SettleFrames = 90
    }
}
