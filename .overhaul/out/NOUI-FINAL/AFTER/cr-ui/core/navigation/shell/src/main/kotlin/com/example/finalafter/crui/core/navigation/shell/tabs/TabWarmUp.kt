package com.example.finalafter.crui.core.navigation.shell.tabs

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.layout.SubcomposeLayoutState
import androidx.compose.ui.platform.LocalView
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Builds the tabs nobody has opened yet while the app is idle, so the first visit to a tab costs
 * what a switch to a kept tab costs instead of composing a whole screen on the frame of the tap.
 *
 * A tab is composed in slices that run after a frame has been drawn and stop before the next one
 * is due, as lazy lists prefetch their rows; a frame never waits for it. Once composed it is laid
 * out parked like any hidden tab, then drawn for one frame under the opaque tab in front, which
 * pays its first draws (glyphs, shaders, images) out of sight, and parked.
 *
 * Its lifecycle stays CREATED until it is shown, so a screen that loads when it is started does
 * not load early.
 */
@Stable
internal class TabWarmUp(private val slots: SubcomposeLayoutState) {

    /** The tab drawn under the one in front for one frame, or -1. Read in layout and layer blocks. */
    var underneath by mutableIntStateOf(-1)
        private set

    suspend fun warm(index: Int, content: @Composable () -> Unit, view: View, onComposed: () -> Unit) {
        val precomposition = slots.createPausedPrecomposition(index, content)
        try {
            while (!precomposition.isComplete) {
                afterFrame(view) { deadline -> precomposition.resume { System.nanoTime() >= deadline } }
            }
            precomposition.apply()
        } catch (e: CancellationException) {
            precomposition.cancel()
            throw e
        }
        onComposed()
        // One frame to lay it out parked, one drawn under the tab in front, then parked again.
        withFrameNanos {}
        underneath = index
        withFrameNanos {}
        withFrameNanos {}
        underneath = -1
    }
}

/**
 * Warms [tabs] that are not in [opened] one at a time, once the shell has drawn its first frames,
 * and adds each to [opened] when it is composed.
 */
@Composable
internal fun WarmUpTabs(
    warming: TabWarmUp,
    tabs: List<Int>,
    opened: SnapshotStateList<Int>,
    content: (index: Int) -> @Composable () -> Unit,
) {
    val view = LocalView.current
    LaunchedEffect(warming, tabs) {
        if (tabs.none { it !in opened }) return@LaunchedEffect
        // The launch's own first frames, and the first content its tab loads, come first.
        delay(SETTLE_MILLIS)
        for (index in tabs) {
            if (index in opened) continue
            warming.warm(index, content(index), view) { if (index !in opened) opened += index }
        }
    }
}

/**
 * Suspends until the next frame is done, then runs [work] with the [System.nanoTime] by which it
 * must return so the frame after it starts on time. A message posted from the frame runs once that
 * frame's layout and draw are done, as the lazy lists' prefetch runs.
 */
private suspend fun afterFrame(view: View, work: (deadlineNanos: Long) -> Unit) {
    val frameNanos = (NANOS_PER_SECOND / (view.display?.refreshRate ?: DEFAULT_REFRESH_RATE)).toLong()
    val frameStart = withFrameNanos { System.nanoTime() }
    suspendCancellableCoroutine { continuation ->
        val slice = Runnable {
            val deadline = frameStart + frameNanos - SLICE_MARGIN_NANOS
            if (System.nanoTime() < deadline) work(deadline)
            continuation.resume(Unit)
        }
        view.post(slice)
        continuation.invokeOnCancellation { view.removeCallbacks(slice) }
    }
}

private const val SETTLE_MILLIS = 600L
private const val NANOS_PER_SECOND = 1_000_000_000f
private const val DEFAULT_REFRESH_RATE = 60f

/** Left over at the end of each slice for whatever else the main thread has to run before the next frame. */
private const val SLICE_MARGIN_NANOS = 1_000_000L
