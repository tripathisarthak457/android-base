package com.example.modulesmodular.core.testing.compose.recomposition

import androidx.compose.runtime.Composer
import androidx.compose.runtime.CompositionTracer
import androidx.compose.runtime.InternalComposeTracingApi
import java.util.concurrent.ConcurrentHashMap
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Counts the composables that execute while a test runs, from the trace marker the Compose
 * compiler puts at the start of every function and lambda that runs rather than skips. A count
 * is per fully qualified name, so `count("AppTabBar")` sums every overload and nested lambda.
 *
 * Apply it as a rule, set the content, [reset] once setup has settled, then drive the journey and
 * read [count], or the frame-by-frame view from [framesUntilIdle].
 */
@OptIn(InternalComposeTracingApi::class)
class RecompositionCounter : TestRule {

    private val executions = ConcurrentHashMap<String, Int>()

    private val tracer = object : CompositionTracer {
        override fun isTraceInProgress() = true

        override fun traceEventStart(key: Int, dirty1: Int, dirty2: Int, info: String) {
            // `info` is "com.acme.Row (Row.kt:42)"; the name before the source position is the key.
            executions.merge(info.substringBefore(" ("), 1, Int::plus)
        }

        override fun traceEventEnd() = Unit
    }

    override fun apply(base: Statement, description: Description): Statement = object : Statement() {
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

    fun snapshot(): Map<String, Int> = HashMap(executions)

    fun count(name: String): Int = executions.entries.sumOf { (executed, times) -> if (executed.matches(name)) times else 0 }
}

/** True when this fully qualified name is [name], or ends in `.name`. */
internal fun String.matches(name: String): Boolean = this == name || endsWith(".$name")
