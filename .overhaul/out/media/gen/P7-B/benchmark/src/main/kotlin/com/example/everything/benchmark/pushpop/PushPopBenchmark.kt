package com.example.everything.benchmark.pushpop

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import com.example.everything.benchmark.TARGET_APP_ID
import com.example.everything.benchmark.backToRoot
import com.example.everything.benchmark.openFirstRow
import com.example.everything.benchmark.startToShell
import org.junit.Rule
import org.junit.Test

/** Open the first row of the first tab and go back with the Back key, five times. */
class PushPopBenchmark {

    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun pushAndPop() = rule.measureRepeated(
        packageName = TARGET_APP_ID,
        metrics = listOf(FrameTimingMetric()),
        iterations = ITERATIONS,
        startupMode = StartupMode.WARM,
        compilationMode = CompilationMode.Partial(),
        setupBlock = { startToShell() },
    ) {
        repeat(PUSHES) {
            openFirstRow()
            backToRoot()
        }
    }

    private companion object {
        const val ITERATIONS = 5
        const val PUSHES = 5
    }
}
