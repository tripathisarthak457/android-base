package com.example.finalafter.crnoui3.buildlogic.guard

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Which tasks run `verifyCodeRules` in a real project. */
class CodeRulesWiringTest {

    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun `the design system's rules run before it builds, so an IDE run shows a component's finding`() {
        val root = ProjectBuilder.builder().withProjectDir(folder.root).build()
        val designSystem = module(root, "core", "designsystem")
        val ui = module(root, "core", "ui")

        assertEquals(setOf("check", "preBuild"), hooked(designSystem))
        assertEquals(setOf("check"), hooked(ui))
    }

    /** The tasks among `check`, `preBuild` and `assemble` that depend on the module's `verifyCodeRules`. */
    private fun hooked(project: Project): Set<String> {
        val candidates = listOf("check", "preBuild", "assemble").map { project.tasks.register(it).get() }
        project.verifyCodeRules()
        val rules = project.tasks.getByName("verifyCodeRules")
        return candidates.filter { task ->
            task.taskDependencies.getDependencies(task).contains(rules)
        }.map { it.name }.toSet()
    }

    private fun module(root: Project, vararg path: String): Project =
        path.fold(root) { parent, name ->
            parent.childProjects[name] ?: ProjectBuilder.builder().withName(name).withParent(parent).build()
        }
}
