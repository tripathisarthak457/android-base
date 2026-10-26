package com.example.finalafter.crui3.benchmark.tabs

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import com.example.finalafter.crui3.benchmark.TARGET_APP_ID
import com.example.finalafter.crui3.benchmark.startToShell
import com.example.finalafter.crui3.benchmark.tabCount
import com.example.finalafter.crui3.benchmark.tapTab
import org.junit.Rule
import org.junit.Test

/** Every tab, twice round, after each has been visited once. */
class TabSwitchBenchmark {

    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun switchTabs() = rule.measureRepeated(
        packageName = TARGET_APP_ID,
        metrics = listOf(FrameTimingMetric()),
        iterations = ITERATIONS,
        startupMode = StartupMode.WARM,
        compilationMode = CompilationMode.Partial(),
        setupBlock = {
            startToShell()
            (1 until tabCount()).forEach(::tapTab)
            tapTab(0)
        },
    ) {
        val tabs = tabCount()
        repeat(2) { (1 until tabs).forEach(::tapTab); tapTab(0) }
    }

    private companion object {
        const val ITERATIONS = 5
    }
}
