package io.example.fitcnet.buildlogic

import java.time.Duration
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.withType

/**
 * How long one module's unit tests may run before the build fails them. A test JVM that stops
 * making progress (a coroutine waiting on a dispatcher no test advances, a latch nothing counts
 * down) otherwise holds the build, and a CI runner, until someone kills it; a failed task names
 * the module instead. The slowest suite here, the design system's, takes about four minutes on a
 * laptop. Raise this when a suite legitimately grows past a third of it.
 */
private val UNIT_TEST_TIME_LIMIT: Duration = Duration.ofMinutes(15)

/** Applied by every convention that gives a module unit tests. */
internal fun Project.limitTestTime() {
    tasks.withType<Test>().configureEach { timeout.set(UNIT_TEST_TIME_LIMIT) }
}
