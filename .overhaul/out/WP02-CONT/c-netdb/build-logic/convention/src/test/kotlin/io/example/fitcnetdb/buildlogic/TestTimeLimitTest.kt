package io.example.fitcnetdb.buildlogic

import java.time.Duration
import org.gradle.api.tasks.testing.Test
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Assert.assertEquals

class TestTimeLimitTest {

    @org.junit.Test
    fun `every test task has the limit, including one registered after the convention ran`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("java")

        project.limitTestTime()
        project.tasks.register("testDebugUnitTest", Test::class.java)

        project.tasks.withType(Test::class.java).forEach { task ->
            assertEquals(task.name, Duration.ofMinutes(15), task.timeout.orNull)
        }
        assertEquals(2, project.tasks.withType(Test::class.java).size)
    }
}
