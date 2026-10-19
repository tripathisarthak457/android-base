package com.example.wp04.everything.core.navigation.back

import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.navigationevent.NavigationEvent
import com.example.wp04.everything.core.navigation.ROBOLECTRIC_SDK
import kotlinx.coroutines.CoroutineScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The texture rendered ahead of a back gesture: taken as a finger lands at the back edge, kept while
 * the touch may still become the gesture, and let go as soon as it turns out to be anything else.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class BackEdgeWarmTest {

    @get:Rule
    val rule = createComposeRule()

    private val host = TestHost()
    private val edges = BackEdges { view, windowX -> windowX < EDGE || windowX >= view.rootView.width - EDGE }
    private lateinit var scope: CoroutineScope

    private fun show() {
        rule.setContent {
            scope = rememberCoroutineScope()
            Box(Modifier.fillMaxSize().backEdgeWarm(host, edges).testTag(HOST))
        }
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
    }

    private fun touch(block: TouchInjectionScope.() -> Unit) {
        rule.onNodeWithTag(HOST).performTouchInput(block)
    }

    private fun warm(): Boolean = rule.runOnUiThread { host.card.textures(Top) }

    @Test
    fun `a touch at the back edge renders the screen into its texture at once`() {
        show()
        touch { down(Offset(2f, centerY)) }
        assertTrue(warm())
    }

    @Test
    fun `a touch away from the edges renders nothing`() {
        show()
        touch { down(center) }
        assertFalse(warm())
    }

    @Test
    fun `where no card can start, as at the root, an edge touch renders nothing`() {
        host.canStart = false
        show()
        touch { down(Offset(width - 2f, centerY)) }
        assertFalse(warm())
    }

    @Test
    fun `a vertical move past the touch slop is a scroll and lets the texture go at once`() {
        show()
        touch {
            down(Offset(2f, centerY))
            moveBy(Offset(0f, viewConfiguration.touchSlop + 4f))
        }
        assertFalse(warm())
    }

    @Test
    fun `a sideways move keeps it, as the start of a back swipe does`() {
        show()
        touch {
            down(Offset(2f, centerY))
            moveBy(Offset(viewConfiguration.touchSlop * 2, 0f))
        }
        assertTrue(warm())
    }

    @Test
    fun `a lift keeps it a moment for a gesture the system may still report, then lets go`() {
        show()
        touch {
            down(Offset(2f, centerY))
            up()
        }
        rule.mainClock.advanceTimeBy(60)
        assertTrue("within the grace", warm())
        rule.mainClock.advanceTimeBy(100)
        assertFalse(warm())
    }

    @Test
    fun `when the system takes the touch and the gesture starts, the texture becomes the card's`() {
        show()
        touch {
            down(Offset(2f, centerY))
            cancel()
        }
        rule.runOnUiThread { host.startCard(NavigationEvent(NavigationEvent.EDGE_LEFT, 0f, 2f, 300f), scope) }
        rule.mainClock.advanceTimeBy(300)

        assertTrue(warm())
        assertNull("drawn ahead no longer, but as the card", rule.runOnUiThread { host.card.prewarmed })
    }

    @Test
    fun `a finger held still at the edge lets go after a second`() {
        show()
        touch { down(Offset(2f, centerY)) }
        rule.mainClock.advanceTimeBy(900)
        assertTrue(warm())
        rule.mainClock.advanceTimeBy(200)
        assertFalse(warm())
    }

    @Test
    fun `a second finger lets go at once`() {
        show()
        touch {
            down(0, Offset(2f, centerY))
            down(1, center)
        }
        assertFalse(warm())
    }

    @Test
    fun `while the card is up a new edge touch changes nothing`() {
        show()
        rule.runOnUiThread { host.startCard(NavigationEvent(NavigationEvent.EDGE_LEFT, 0.3f, 2f, 300f), scope) }
        touch { down(Offset(2f, centerY)) }
        assertEquals(null, rule.runOnUiThread { host.card.prewarmed })
        assertTrue(warm())
    }

    private class TestHost : BackCardHost {
        override val card = BackCardState()
        var canStart = true

        override fun canStartCard(): Boolean = canStart && !card.active

        override fun warmCard(): Boolean {
            if (!canStartCard()) return false
            card.warm(Top)
            return true
        }

        override fun startCard(event: NavigationEvent, scope: CoroutineScope) =
            card.start(event, subject = Top, beneath = setOf(Under), midPush = false, scope = scope)

        override suspend fun cancelCard() = card.cancel(midPush = false, fullWidth = true)

        override suspend fun commitCard(pop: (Int) -> Int, releasedNanos: Long, scope: CoroutineScope) =
            card.commitPop(BackGate(), spring(), pop, releasedNanos, scope)
    }

    private companion object {
        const val HOST = "host"
        const val Top = "top"
        const val Under = "under"
        const val EDGE = 20f
    }
}
