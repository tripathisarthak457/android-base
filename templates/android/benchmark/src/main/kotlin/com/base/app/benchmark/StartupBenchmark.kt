package com.base.app.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import org.junit.Rule
import org.junit.Test

/**
 * Measures cold start, with and without the baseline profile.
 *
 * ```
 * ./gradlew :benchmark:connectedBenchmarkAndroidTest
 * ```
 */
class StartupBenchmark {

    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun startupWithoutProfile() = measure(CompilationMode.None())

    @Test
    fun startupWithProfile() = measure(CompilationMode.Partial())

    private fun measure(mode: CompilationMode) = rule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(StartupTimingMetric()),
        iterations = ITERATIONS,
        startupMode = StartupMode.COLD,
        compilationMode = mode,
    ) {
        pressHome()
        startActivityAndWait()
    }

    private companion object {
        const val PACKAGE_NAME = "com.base.app.dev"

        /** Enough for the median to be stable; more mostly buys thermal throttling. */
        const val ITERATIONS = 10
    }
}
