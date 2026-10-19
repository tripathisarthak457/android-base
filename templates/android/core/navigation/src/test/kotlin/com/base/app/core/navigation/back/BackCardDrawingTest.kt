package com.base.app.core.navigation.back

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
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEvent
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.navigation.AppBackStack
import com.base.app.core.navigation.FeedTab
import com.base.app.core.navigation.Post
import com.base.app.core.navigation.ROBOLECTRIC_SDK
import com.base.app.core.navigation.stack.StackHost
import com.base.app.core.navigation.testRegistry
import com.base.app.core.navigation.transition.ScreenMotion
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * What the back card leaves on screen: the page beneath is drawn only where the card does not cover
 * it, so every pixel outside the card, its rounded corners included, must still show that page
 * under the scrim, and none of it may show through the card.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BackCardDrawingTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val input = DirectNavigationEventInput()

    private val registry = testRegistry { key ->
        val fill = if (key == FeedTab) Beneath else Card
        Box(Modifier.fillMaxSize().background(fill).testTag(key.toString()))
    }

    @Test
    fun `the page beneath shows everywhere around the card and nowhere through it`() {
        rule.setContent {
            AppTheme {
                StackHost(stack = AppBackStack(mutableStateListOf(FeedTab, Post(1))), registry = registry, motion = ScreenMotion())
            }
        }
        rule.runOnIdle { rule.activity.navigationEventDispatcher.addInput(input) }
        rule.waitForIdle()
        rule.runOnIdle {
            input.backStarted(event(0f))
            input.backProgressed(event(0.6f))
        }
        rule.waitForIdle()

        val card = rule.onNodeWithTag(Post(1).toString(), useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val image = rule.onRoot().captureToImage().asAndroidBitmap()
        val dimmed = dimmed(Beneath)

        val outside = pointsAround(card, image.width, image.height)
        assertTrue("the card leaves room around it", outside.size > 20)
        outside.forEach { (x, y) -> assertColor("beneath, at $x,$y outside $card", dimmed, image, x, y) }
        // Just inside the card's bounding box, at its top corners: outside the rounded corner itself.
        assertColor("beneath, in the card's top start corner", dimmed, image, (card.left + 2).roundToInt(), (card.top + 2).roundToInt())
        assertColor("beneath, in the card's top end corner", dimmed, image, (card.right - 3).roundToInt(), (card.top + 2).roundToInt())
        assertColor("the card, at its centre", Card, image, card.center.x.roundToInt(), card.center.y.roundToInt())
        assertColor("the card, at its start edge", Card, image, (card.left + 3).roundToInt(), card.center.y.roundToInt())
    }

    private fun event(progress: Float) = NavigationEvent(
        swipeEdge = NavigationEvent.EDGE_LEFT,
        progress = progress,
        touchX = progress * 400f,
        touchY = 600f,
        frameTimeMillis = rule.mainClock.currentTime,
    )

    /** A grid over the window, less the card's bounds and a pixel around them for the edges' anti-aliasing. */
    private fun pointsAround(card: Rect, width: Int, height: Int): List<Pair<Int, Int>> {
        val points = mutableListOf<Pair<Int, Int>>()
        for (x in 0 until width step 7) {
            for (y in 0 until height step 13) {
                val near = x >= card.left - 2 && x <= card.right + 2 && y >= card.top - 2 && y <= card.bottom + 2
                if (!near) points += x to y
            }
        }
        return points
    }

    private fun dimmed(color: Color): Color = Color(
        red = color.red * (1f - BackCard.SCRIM_ALPHA),
        green = color.green * (1f - BackCard.SCRIM_ALPHA),
        blue = color.blue * (1f - BackCard.SCRIM_ALPHA),
    )

    private fun assertColor(what: String, expected: Color, image: Bitmap, x: Int, y: Int) {
        val pixel = image.getPixel(x.coerceIn(0, image.width - 1), y.coerceIn(0, image.height - 1))
        val actual = Color(pixel)
        val close = abs(actual.red - expected.red) < TOLERANCE &&
            abs(actual.green - expected.green) < TOLERANCE &&
            abs(actual.blue - expected.blue) < TOLERANCE
        assertTrue("$what: expected $expected, was $actual", close)
    }

    private companion object {
        val Beneath = Color(0xFFD03030)
        val Card = Color(0xFF3050D0)
        const val TOLERANCE = 0.05f
    }
}
