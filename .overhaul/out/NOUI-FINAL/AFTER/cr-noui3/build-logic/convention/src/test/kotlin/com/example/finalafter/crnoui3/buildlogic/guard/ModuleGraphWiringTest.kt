package com.example.finalafter.crnoui3.buildlogic.guard

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** What the guard collects from a real project, which the rule tests in [ModuleGraphTest] take as given. */
class ModuleGraphWiringTest {

    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun `test, variant and fixture edges are checked, and tool configurations are not`() {
        val root = ProjectBuilder.builder().withProjectDir(folder.root).build()
        val sample = module(root, "feature", "sample")
        val edges = mapOf(
            "testImplementation" to "one",
            "debugImplementation" to "two",
            "testFixturesApi" to "three",
            "androidTestImplementation" to "four",
            "lintChecks" to "five",
            "ksp" to "six",
        )
        edges.forEach { (configuration, target) ->
            sample.configurations.create(configuration)
            sample.dependencies.add(configuration, module(root, "feature", target))
        }

        val guard = guardOf(sample)

        assertEquals(
            listOf(":feature:four", ":feature:one", ":feature:three", ":feature:two"),
            guard.dependencies.get(),
        )
        val error = assertThrows(GradleException::class.java) { guard.verify() }
        assertTrue(error.message!!, error.message!!.contains("  :feature:sample -> :feature:one\n"))
    }

    @Test
    fun `the design system's libraries are collected from every checked configuration and judged`() {
        val root = ProjectBuilder.builder().withProjectDir(folder.root).build()
        val designSystem = module(root, "core", "designsystem")
        val libraries = mapOf(
            "api" to "androidx.compose.foundation:foundation:1.12.0",
            "implementation" to "io.ktor:ktor-client-core:3.6.0",
            "testImplementation" to "junit:junit:4.13.2",
            "lintChecks" to "com.example:lint-rules:1.0.0",
        )
        libraries.forEach { (configuration, library) ->
            designSystem.configurations.create(configuration)
            designSystem.dependencies.add(configuration, library)
        }

        val guard = guardOf(designSystem)

        assertEquals(
            listOf("androidx.compose.foundation:foundation", "io.ktor:ktor-client-core", "junit:junit"),
            guard.libraries.get(),
        )
        val error = assertThrows(GradleException::class.java) { guard.verify() }
        assertTrue(error.message!!, error.message!!.contains("  :core:designsystem -> io.ktor:ktor-client-core\n"))
    }

    @Test
    fun `a module without a library allowlist collects no libraries`() {
        val root = ProjectBuilder.builder().withProjectDir(folder.root).build()
        val ui = module(root, "core", "ui")
        ui.configurations.create("implementation")
        ui.dependencies.add("implementation", "io.ktor:ktor-client-core:3.6.0")

        assertEquals(emptyList<String>(), guardOf(ui).libraries.get())
    }

    @Test
    fun `a pure module that applies an Android or Compose plugin fails`() {
        val root = ProjectBuilder.builder().withProjectDir(folder.root).build()
        val guard = guardOf(module(root, "feature", "orders", "api"))
        // Neither plugin is on this test's classpath, so the result of the plugin lookup is given here.
        guard.androidPlugins.set(listOf("org.jetbrains.kotlin.plugin.compose"))

        val error = assertThrows(GradleException::class.java) { guard.verify() }
        assertTrue(
            error.message!!,
            error.message!!.startsWith(
                ":feature:orders:api is a pure Kotlin module and may not apply org.jetbrains.kotlin.plugin.compose.",
            ),
        )
    }

    private fun module(root: Project, vararg path: String): Project =
        path.fold(root) { parent, name ->
            parent.childProjects[name] ?: ProjectBuilder.builder().withName(name).withParent(parent).build()
        }

    private fun guardOf(project: Project): VerifyModuleDependenciesTask {
        project.verifyModuleDependencies()
        // Runs the afterEvaluate block that collects the declared dependencies.
        (project as ProjectInternal).evaluate()
        return project.tasks.getByName("verifyModuleDependencies") as VerifyModuleDependenciesTask
    }
}
