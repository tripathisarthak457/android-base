package com.example.everything.benchmark.scroll

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import com.example.everything.benchmark.TARGET_APP_ID
import com.example.everything.benchmark.flingPrimaryList
import com.example.everything.benchmark.startToShell
import org.junit.Rule
import org.junit.Test

/** Flings the first tab's root list down and up, under the floating bar. */
class ScrollBenchmark {

    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun flingRootList() = rule.measureRepeated(
        packageName = TARGET_APP_ID,
        metrics = listOf(FrameTimingMetric()),
        iterations = ITERATIONS,
        startupMode = StartupMode.WARM,
        compilationMode = CompilationMode.Partial(),
        setupBlock = { startToShell() },
    ) {
        repeat(FLINGS) { flingPrimaryList() }
    }

    private companion object {
        const val ITERATIONS = 5
        const val FLINGS = 3
    }
}
