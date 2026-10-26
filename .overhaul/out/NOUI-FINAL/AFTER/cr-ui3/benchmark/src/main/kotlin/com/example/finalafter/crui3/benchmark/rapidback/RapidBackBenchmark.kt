package com.example.finalafter.crui3.benchmark.rapidback

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import com.example.finalafter.crui3.benchmark.TARGET_APP_ID
import com.example.finalafter.crui3.benchmark.assertAtRoot
import com.example.finalafter.crui3.benchmark.openFirstRow
import com.example.finalafter.crui3.benchmark.openRelated
import com.example.finalafter.crui3.benchmark.pressBackBurst
import com.example.finalafter.crui3.benchmark.startToShell
import org.junit.Rule
import org.junit.Test

/**
 * Three screens deep, then three Back presses in one burst, so two arrive while the first pop is
 * still moving. The presses must be coalesced into one pop to the root: the app stays in front, on
 * the root list, with no frame of a half-finished screen in between.
 */
class RapidBackBenchmark {

    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun backBurst() = rule.measureRepeated(
        packageName = TARGET_APP_ID,
        metrics = listOf(FrameTimingMetric()),
        iterations = ITERATIONS,
        startupMode = StartupMode.WARM,
        compilationMode = CompilationMode.Partial(),
        setupBlock = { startToShell() },
    ) {
        repeat(BURSTS) {
            openFirstRow()
            openRelated()
            openRelated()
            pressBackBurst(DEPTH - 1)
            assertAtRoot()
            device.waitForIdle()
        }
    }

    private companion object {
        const val ITERATIONS = 5
        const val BURSTS = 3
        const val DEPTH = 4
    }
}
