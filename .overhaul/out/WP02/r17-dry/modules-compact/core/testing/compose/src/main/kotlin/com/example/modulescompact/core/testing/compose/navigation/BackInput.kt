package com.example.modulescompact.core.testing.compose.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEvent

/**
 * Sends Back to [activity] the way the system does: through its navigation-event dispatcher, so
 * every handler the app registered sees it in the order the app gave them. Create it on the main
 * thread, after the content is set.
 */
class BackInput(activity: ComponentActivity) {

    private val input = DirectNavigationEventInput().also(activity.navigationEventDispatcher::addInput)

    /** Back with no gesture to preview: the key, the three-button bar, or a swipe too short to start. */
    fun press() = input.backCompleted()

    fun start(edge: Int = NavigationEvent.EDGE_LEFT, frameTimeMillis: Long = 0) =
        input.backStarted(NavigationEvent(swipeEdge = edge, frameTimeMillis = frameTimeMillis))

    fun progress(fraction: Float, edge: Int = NavigationEvent.EDGE_LEFT, frameTimeMillis: Long = 0) =
        input.backProgressed(NavigationEvent(swipeEdge = edge, progress = fraction, frameTimeMillis = frameTimeMillis))

    fun cancel() = input.backCancelled()

    fun complete() = input.backCompleted()
}

/**
 * A predictive-back gesture one frame per event, as a finger produces it: start, [steps] progress
 * events up to [reach], then a commit or, with `commit = false`, a cancel. The clock must be paused
 * (`mainClock.autoAdvance = false`) so each event lands in its own frame.
 */
fun ComposeTestRule.swipeBack(
    back: BackInput,
    steps: Int = 30,
    reach: Float = 1f,
    commit: Boolean = true,
    edge: Int = NavigationEvent.EDGE_LEFT,
) {
    runOnUiThread { back.start(edge, mainClock.currentTime) }
    mainClock.advanceTimeByFrame()
    for (step in 1..steps) {
        runOnUiThread { back.progress(reach * step / steps, edge, mainClock.currentTime) }
        mainClock.advanceTimeByFrame()
    }
    runOnUiThread { if (commit) back.complete() else back.cancel() }
}
