package com.example.lean.core.navigation.back

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEvent
import com.example.lean.core.designsystem.theme.AppTheme
import com.example.lean.core.navigation.AppBackStack
import com.example.lean.core.navigation.FeedTab
import com.example.lean.core.navigation.NavCommand
import com.example.lean.core.navigation.Post
import com.example.lean.core.navigation.ROBOLECTRIC_SDK
import com.example.lean.core.navigation.stack.StackHost
import com.example.lean.core.navigation.testRegistry
import com.example.lean.core.navigation.transition.ScreenMotion
import kotlin.math.abs
import kotlin.math.roundToInt
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * A back swipe that starts while the pushed screen is still sliding in (requirement 32), from the
 * right edge as people swipe with the right thumb. Before the shell drew its own card, Navigation 3
 * sought its pop transition from wherever the push was, and both screens were drawn through each
 * other for as long as the finger stayed down.
 *
 * The pushed screen here paints nothing behind its content, as a scaffold told that the frame has
 * painted its background does not, so all that keeps the page beneath from showing through is the
 * card itself. On every frame of the drag, every pixel inside the card must be the card's own.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BackCardDuringPushDrawingTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val input = DirectNavigationEventInput()

    private val registry = testRegistry { key ->
        if (key == FeedTab) {
            Box(Modifier.fillMaxSize().background(Beneath).testTag(key.toString()))
        } else {
            Box(Modifier.fillMaxSize().testTag(key.toString()))
        }
    }

    @Test
    fun `a swipe during a push shows one opaque card on every frame, never the page beneath through it`() {
        var cardColour = Color.Unspecified
        val stack = AppBackStack(mutableStateListOf(FeedTab))
        rule.setContent {
            AppTheme {
                cardColour = AppTheme.colors.background
                StackHost(stack = stack, registry = registry, motion = ScreenMotion())
            }
        }
        rule.runOnIdle { rule.activity.navigationEventDispatcher.addInput(input) }
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        val width = rule.onRoot().fetchSemanticsNode().boundsInRoot.width
        rule.runOnIdle {
            stack.apply(NavCommand.Navigate(Post(1)))
            stack.gate.changed(StackChange.Push)
        }
        var pushFrames = 0
        while ((pushed()?.left ?: width) > width * (1f - PUSH_CAUGHT_AT) && pushFrames++ < MAX_PUSH_FRAMES) frame()
        val caught = pushed()?.left
        assertTrue("the push is under way when the swipe starts, its page's left edge at $caught", caught != null && caught > 0f && caught < width)

        rule.runOnIdle {
            input.backStarted(event(0f))
            input.backProgressed(event(FIRST_PROGRESS))
        }
        repeat(HELD_FRAMES) { frame ->
            if (frame == HELD_FRAMES / 2) rule.runOnIdle { input.backProgressed(event(LATER_PROGRESS)) }
            frame()
            val (card, image) = capture()
            val inside = interior(card, image.width, image.height)
            assertTrue("frame $frame: the card $card is on screen", inside.isNotEmpty())
            inside.forEach { (x, y) -> assertColor("frame $frame, inside the card $card at $x,$y", cardColour, image, x, y) }
        }

        // The gesture drives the card, and the page beneath is drawn around it: the frames above saw
        // that page covered, not missing.
        val (card, image) = capture()
        assertTrue("the card has shrunk: $card in a window $width wide", card.width < width - 1f)
        val around = (card.right + 3f).roundToInt() until image.width
        assertTrue("the card leaves a strip at its right, $card", !around.isEmpty())
        around.forEach { x ->
            val pixel = Color(image.getPixel(x, card.center.y.roundToInt()))
            assertTrue("the page beneath at $x beside the card, was $pixel", pixel.red > 2f * pixel.green && pixel.red > 2f * pixel.blue)
        }

        rule.runOnIdle { input.backCancelled() }
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
    }

    /** One frame, and the work it dispatched: an animation launched in a frame takes its first step in the next. */
    private fun frame() {
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()
    }

    private fun pushed(): Rect? =
        rule.onAllNodesWithTag(Post(1).toString(), useUnmergedTree = true).fetchSemanticsNodes().firstOrNull()?.boundsInRoot

    private fun capture(): Pair<Rect, Bitmap> {
        val card = rule.onNodeWithTag(Post(1).toString(), useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        return card to rule.onRoot().captureToImage().asAndroidBitmap()
    }

    private fun event(progress: Float) = NavigationEvent(
        swipeEdge = NavigationEvent.EDGE_RIGHT,
        progress = progress,
        touchX = 400f * (1f - progress),
        touchY = 600f,
        frameTimeMillis = rule.mainClock.currentTime,
    )

    /**
     * A grid over the part of the card in the window, kept a corner radius in from the card's edges
     * so its rounded corners are not in it.
     */
    private fun interior(card: Rect, width: Int, height: Int): List<Pair<Int, Int>> {
        val inset = with(rule.density) { BackCard.Corner.toPx() } + 2f
        val right = minOf(card.right - inset, width - 1f)
        val bottom = minOf(card.bottom - inset, height - 1f)
        val points = mutableListOf<Pair<Int, Int>>()
        var x = maxOf(card.left + inset, 0f)
        while (x <= right) {
            var y = maxOf(card.top + inset, 0f)
            while (y <= bottom) {
                points += x.roundToInt() to y.roundToInt()
                y += 13f
            }
            x += 7f
        }
        return points
    }

    private fun assertColor(what: String, expected: Color, image: Bitmap, x: Int, y: Int) {
        val actual = Color(image.getPixel(x, y))
        val close = abs(actual.red - expected.red) < TOLERANCE &&
            abs(actual.green - expected.green) < TOLERANCE &&
            abs(actual.blue - expected.blue) < TOLERANCE
        assertTrue("$what: expected $expected, was $actual", close)
    }

    private companion object {
        /** Saturated, so a tenth of it mixed into the card's colour, light or dark, moves a channel by more than the tolerance. */
        val Beneath = Color(0xFFD03030)
        const val TOLERANCE = 0.05f

        /** How far in the pushed page is when the swipe starts: about a third, where the user's own swipe caught it. */
        const val PUSH_CAUGHT_AT = 0.3f
        const val MAX_PUSH_FRAMES = 30

        /** About two thirds of a second of the drag, checked frame by frame. */
        const val HELD_FRAMES = 40
        const val FIRST_PROGRESS = 0.2f
        const val LATER_PROGRESS = 0.45f
    }
}
