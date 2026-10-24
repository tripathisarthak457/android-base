package com.example.deskstandard.core.navigation.recomposition

import androidx.compose.runtime.Composer
import androidx.compose.runtime.CompositionTracer
import androidx.compose.runtime.InternalComposeTracingApi
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import java.util.concurrent.ConcurrentHashMap
import org.junit.Assert.fail
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Counts executed composables while a test runs, from the trace markers the Compose compiler emits
 * at the start of every function and lambda that runs rather than skips. Install it as a rule,
 * before content is set; [reset] after setup.
 */
@OptIn(InternalComposeTracingApi::class)
class RecompositionCounter : TestRule {

    private val executions = ConcurrentHashMap<String, Int>()

    private val tracer = object : CompositionTracer {
        override fun isTraceInProgress() = true

        override fun traceEventStart(key: Int, dirty1: Int, dirty2: Int, info: String) {
            executions.merge(info.substringBefore(" ("), 1, Int::plus)
        }

        override fun traceEventEnd() = Unit
    }

    override fun apply(base: Statement, description: Description) = object : Statement() {
        override fun evaluate() {
            Composer.setTracer(tracer)
            try {
                base.evaluate()
            } finally {
                Composer.setTracer(null)
            }
        }
    }

    fun reset() = executions.clear()

    fun snapshot(): Map<String, Int> = HashMap(executions)

    /** Executions of composables whose name ends with [name], such as `StackLayout` or `AppTabBar`. */
    fun count(name: String): Int = executions.entries.sumOf { (fq, n) -> if (fq == name || fq.endsWith(".$name")) n else 0 }
}

/**
 * Steps frames, with the clock's auto-advance off, until nothing is pending, and returns what
 * executed in each frame.
 */
fun ComposeContentTestRule.framesUntilIdle(counter: RecompositionCounter, maxFrames: Int = 600): List<Map<String, Int>> {
    val frames = ArrayList<Map<String, Int>>()
    do {
        val before = counter.snapshot()
        mainClock.advanceTimeByFrame()
        val after = counter.snapshot()
        frames += after.mapNotNull { (name, n) -> (n - (before[name] ?: 0)).takeIf { it > 0 }?.let { name to it } }.toMap()
    } while (hasPendingWork() && frames.size < maxFrames)
    return frames
}

/**
 * Fails when any frame between the first that composed and the last ran a composable not in
 * [allowed]: an animation's frames must move layers and redraw, never compose. The message names
 * the frame and its busiest composables.
 */
fun List<Map<String, Int>>.assertQuietBetweenEnds(allowed: Set<String> = emptySet()) {
    val first = indexOfFirst { it.isNotEmpty() }
    val last = indexOfLast { it.isNotEmpty() }
    for (index in first + 1 until last) {
        val ran = this[index].filterKeys { name -> allowed.none { name == it || name.endsWith(".$it") } }
        if (ran.isNotEmpty()) {
            val top = ran.entries.sortedByDescending { it.value }.take(10).joinToString { "${it.key} ×${it.value}" }
            fail("Frame $index of ${size - 1} composed: $top")
        }
    }
}
