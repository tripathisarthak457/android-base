package com.base.app.benchmark.recomposition

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import com.base.app.benchmark.TARGET_APP_ID
import com.base.app.benchmark.backToRoot
import com.base.app.benchmark.openFirstRow
import com.base.app.benchmark.slowEdgeSwipeBack
import com.base.app.benchmark.startToShell
import com.base.app.benchmark.tabCount
import com.base.app.benchmark.tapTab
import org.junit.Rule
import org.junit.Test

/**
 * How often, and for how long, the app recomposes during the shell's journeys, from the
 * `Compose:recompose` sections the Compose runtime traces in every build. Reported, not gated: the
 * per-transition budgets are gated by `perf.sh recompose` and the Robolectric journeys, and this
 * shows where a regression went on a real device.
 */
@OptIn(ExperimentalMetricApi::class)
class CompositionCountBenchmark {

    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun tabSwitch() = measure(setup = { (1 until tabCount()).forEach(::tapTab); tapTab(0) }) {
        repeat(ROUNDS) {
            tapTab(1)
            tapTab(0)
        }
    }

    @Test
    fun pushAndPop() = measure {
        repeat(ROUNDS) {
            openFirstRow()
            backToRoot()
        }
    }

    @Test
    fun backGesture() = measure {
        openFirstRow()
        slowEdgeSwipeBack(cancel = true)
        slowEdgeSwipeBack()
    }

    private fun measure(
        setup: MacrobenchmarkScope.() -> Unit = {},
        journey: MacrobenchmarkScope.() -> Unit,
    ) = rule.measureRepeated(
        packageName = TARGET_APP_ID,
        metrics = listOf(
            TraceSectionMetric(RECOMPOSE, TraceSectionMetric.Mode.Count),
            TraceSectionMetric(RECOMPOSE, TraceSectionMetric.Mode.Sum),
        ),
        iterations = ITERATIONS,
        startupMode = StartupMode.WARM,
        compilationMode = CompilationMode.Partial(),
        setupBlock = {
            startToShell()
            setup()
        },
        measureBlock = journey,
    )

    private companion object {
        const val RECOMPOSE = "Compose:recompose"
        const val ITERATIONS = 5
        const val ROUNDS = 3
    }
}
