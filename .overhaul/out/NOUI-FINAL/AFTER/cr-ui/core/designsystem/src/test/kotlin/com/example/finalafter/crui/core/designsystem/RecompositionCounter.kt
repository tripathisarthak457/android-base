package com.example.finalafter.crui.core.designsystem

import androidx.compose.runtime.Composer
import androidx.compose.runtime.CompositionTracer
import androidx.compose.runtime.InternalComposeTracingApi
import java.util.concurrent.ConcurrentHashMap
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Counts the composable functions that ran, rather than skipped, while a test runs. It reads the
 * trace markers the Compose compiler puts at the start of every composable function and lambda,
 * so it sees what a recomposition really executed. Apply it before `setContent`, and [reset] after
 * the setup the test does not want counted.
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

    /** How many times composables whose name ends in [name] ran since the last [reset]. */
    fun count(name: String): Int =
        executions.entries.sumOf { (function, times) -> if (function == name || function.endsWith(".$name")) times else 0 }

    /** Every composable that ran since the last [reset], for a failure message. */
    fun executed(): Map<String, Int> = HashMap(executions)
}
