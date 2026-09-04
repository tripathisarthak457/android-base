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
 * Two tests rather than one, because the number that matters is the *difference*. An absolute
 * startup figure is meaningless across devices, thermal states and Android versions; the ratio
 * between the two compilation modes tells you whether the profile is actually doing anything,
 * and catches the case where it silently stopped being applied.
 *
 * ```
 * ./gradlew :benchmark:connectedBenchmarkAndroidTest
 * ```
 *
 * Run it on a physical device with a stable thermal state. Emulator numbers are not comparable
 * between runs and will send you chasing regressions that do not exist.
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
