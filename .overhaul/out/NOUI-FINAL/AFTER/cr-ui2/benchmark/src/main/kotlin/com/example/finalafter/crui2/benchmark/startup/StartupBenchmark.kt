package com.example.finalafter.crui2.benchmark.startup

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import com.example.finalafter.crui2.benchmark.TARGET_APP_ID
import com.example.finalafter.crui2.benchmark.startToShell
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Cold and warm start to the tab shell, without a profile (what a sideloaded build gets) and with the
 * baseline profile (what a Play install gets).
 */
@RunWith(Parameterized::class)
class StartupBenchmark(
    private val startupMode: StartupMode,
    private val compilationMode: CompilationMode,
) {

    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun startup() = rule.measureRepeated(
        packageName = TARGET_APP_ID,
        metrics = listOf(StartupTimingMetric()),
        iterations = ITERATIONS,
        startupMode = startupMode,
        compilationMode = compilationMode,
    ) {
        pressHome()
        startToShell()
    }

    companion object {
        /** Enough for the median to be stable; more mostly buys thermal throttling. */
        private const val ITERATIONS = 10

        @JvmStatic
        @Parameterized.Parameters(name = "{0} {1}")
        fun parameters(): List<Array<Any>> =
            listOf(StartupMode.COLD, StartupMode.WARM).flatMap { mode ->
                listOf(CompilationMode.None(), CompilationMode.Partial()).map { arrayOf(mode, it) }
            }
    }
}
