package com.example.deskstandard.core.testing.compose.recomposition

import androidx.compose.ui.test.junit4.ComposeTestRule

/**
 * Advances the main clock one frame at a time until nothing is pending, and returns what executed
 * in each frame. The first entry is the frame the journey's state change landed in. Needs
 * `mainClock.autoAdvance = false`, or the clock runs ahead of the frames counted here.
 */
fun ComposeTestRule.framesUntilIdle(counter: RecompositionCounter, maxFrames: Int = 600): List<Map<String, Int>> =
    buildList {
        // A state change the test made reaches composition through the main thread's queue. With
        // the clock paused, waiting for idle delivers it without advancing time.
        waitForIdle()
        var frames = 0
        do {
            val before = counter.snapshot()
            mainClock.advanceTimeByFrame()
            add(counter.snapshot().since(before))
        } while (hasPendingWork() && ++frames < maxFrames)
    }

/**
 * Fails when a frame between the first and the last executed anything outside [allowed]. The first
 * frame holds the state change and the last the settle; an animation in between should run in
 * layout and draw only, so a composable executing there recomposes once per frame.
 */
fun List<Map<String, Int>>.assertQuietBetweenEnds(allowed: Set<String> = emptySet()) {
    val noisy = withIndex().toList().drop(1).dropLast(1).mapNotNull { (index, frame) ->
        val executed = frame.filterKeys { name -> allowed.none(name::matches) }
        if (executed.isEmpty()) null else index to executed
    }
    if (noisy.isEmpty()) return

    throw AssertionError(
        buildString {
            appendLine("${noisy.size} of the ${size - 2} frames between the first and the last recomposed:")
            noisy.forEach { (index, executed) ->
                val top = executed.entries.sortedByDescending { it.value }.take(TOP).joinToString { "${it.key} ×${it.value}" }
                appendLine("  frame $index: $top")
            }
        },
    )
}

private fun Map<String, Int>.since(before: Map<String, Int>): Map<String, Int> =
    mapValues { (name, times) -> times - (before[name] ?: 0) }.filterValues { it > 0 }

private const val TOP = 10
