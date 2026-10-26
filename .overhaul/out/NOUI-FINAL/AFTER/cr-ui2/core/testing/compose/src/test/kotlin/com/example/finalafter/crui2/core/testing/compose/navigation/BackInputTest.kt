package com.example.finalafter.crui2.core.testing.compose.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventHandler
import androidx.navigationevent.NavigationEventInfo
import com.example.finalafter.crui2.core.testing.compose.TEST_SDK
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TEST_SDK])
class BackInputTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val received = mutableListOf<String>()

    private val handler = object : NavigationEventHandler<NavigationEventInfo.None>(NavigationEventInfo.None, true) {
        override fun onBackStarted(event: NavigationEvent) {
            received += "started edge=${event.swipeEdge}"
        }

        override fun onBackProgressed(event: NavigationEvent) {
            received += "progress ${event.progress}"
        }

        override fun onBackCancelled() {
            received += "cancelled"
        }

        override fun onBackCompleted() {
            received += "completed"
        }
    }

    @Test
    fun `a press reaches the handler as a completed Back with no gesture`() {
        val back = install()

        compose.runOnUiThread { back.press() }

        assertEquals(listOf("completed"), received)
    }

    @Test
    fun `a swipe arrives one progress event per frame, from the edge it started at`() {
        val back = install()
        compose.mainClock.autoAdvance = false

        compose.swipeBack(back, steps = 4, reach = 0.5f, commit = false, edge = NavigationEvent.EDGE_RIGHT)

        assertEquals(
            listOf(
                "started edge=${NavigationEvent.EDGE_RIGHT}",
                "progress 0.125",
                "progress 0.25",
                "progress 0.375",
                "progress 0.5",
                "cancelled",
            ),
            received,
        )
    }

    private fun install(): BackInput {
        lateinit var back: BackInput
        compose.runOnUiThread {
            compose.activity.navigationEventDispatcher.addHandler(handler)
            back = BackInput(compose.activity)
        }
        return back
    }
}
