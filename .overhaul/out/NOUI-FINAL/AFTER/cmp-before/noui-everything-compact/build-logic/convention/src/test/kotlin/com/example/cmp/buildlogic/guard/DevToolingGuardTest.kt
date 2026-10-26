package com.example.cmp.buildlogic.guard

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DevToolingGuardTest {

    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun `a release that reaches the inspector through another module fails, naming it`() {
        val root = ProjectBuilder.builder().withProjectDir(folder.root).build()
        val devtools = module(root, "core", "devtools")
        val ui = module(root, "core", "ui").also { it.dependencies.add("implementation", devtools) }
        val app = module(root, "app").also { it.dependencies.add("implementation", ui) }

        val error = assertThrows(GradleException::class.java) { guardOf(app).verify() }

        assertTrue(error.message!!, error.message!!.startsWith("prodRelease depends on :core:devtools."))
    }

    @Test
    fun `a release without dev tooling passes`() {
        val root = ProjectBuilder.builder().withProjectDir(folder.root).build()
        module(root, "core", "devtools")
        val ui = module(root, "core", "ui")
        val app = module(root, "app").also { it.dependencies.add("implementation", ui) }
        val guard = guardOf(app)
        // Gradle creates an output's directory before the action runs; called directly, it does not.
        guard.marker.get().asFile.parentFile.mkdirs()

        guard.verify()

        assertEquals("ok", guard.marker.get().asFile.readText())
    }

    private fun module(root: Project, vararg path: String): Project =
        path.fold(root) { parent, name ->
            parent.childProjects[name] ?: ProjectBuilder.builder().withName(name).withParent(parent).build()
        }.also { it.pluginManager.apply("java-library") }

    private fun guardOf(app: Project): VerifyNoDevToolingTask =
        app.tasks.register<VerifyNoDevToolingTask>("verifyProdReleaseCarriesNoDevTooling") {
            variantName.set("prodRelease")
            dependencyGraph.set(
                app.configurations.getByName("runtimeClasspath").incoming.resolutionResult.rootComponent,
            )
            marker.set(app.layout.buildDirectory.file("guards/$name.ok"))
        }.get()
}
