package com.base.app.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import org.junit.Rule
import org.junit.Test

/** Open the first row and swipe back from the edge: the predictive back animation, end to end. */
class PredictiveBackBenchmark {

    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun swipeBack() = rule.measureRepeated(
        packageName = TARGET_APP_ID,
        metrics = listOf(FrameTimingMetric()),
        iterations = ITERATIONS,
        startupMode = StartupMode.WARM,
        compilationMode = CompilationMode.Partial(),
        setupBlock = { startToShell() },
    ) {
        repeat(SWIPES) {
            openFirstRow()
            edgeSwipeBack()
            assertAtRoot()
            device.waitForIdle()
        }
    }

    private companion object {
        const val ITERATIONS = 5
        const val SWIPES = 5
    }
}
