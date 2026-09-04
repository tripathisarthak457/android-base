package com.base.app.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.register

/**
 * Enforces the layering declared in `settings.gradle.kts`, per module, at build time.
 *
 * ```
 *   :app / :catalog   → anything
 *   :feature:*        → :core:*, :data:*            (never another :feature:*)
 *   :data:*           → :core:*                     (never another :data:*)
 *   :core:*           → :core:*                     (never :data:* or :feature:*)
 * ```
 *
 * The two sibling rules are the load-bearing ones. A single `:feature:cart → :feature:catalog`
 * edge is harmless on the day it is added and, eighteen months later, is the reason nothing can
 * be built or tested in isolation and every change recompiles everything. It always arrives as a
 * one-line convenience in a pull request that is about something else, which is why this is a
 * build failure rather than a review checklist item.
 *
 * The check runs per project and reads only that project's own dependencies, so it stays
 * compatible with configuration caching and project isolation.
 */
internal fun Project.verifyModuleDependencies() {
    val tier = ModuleTier.of(path) ?: return

    val task = tasks.register<VerifyModuleDependenciesTask>("verifyModuleDependencies") {
        group = "verification"
        description = "Fails if this module depends on one it is not allowed to see."
        modulePath.set(path)
        tierName.set(tier.name)
    }

    afterEvaluate {
        val declared = DEPENDENCY_CONFIGURATIONS
            .mapNotNull { configurations.findByName(it) }
            .flatMap { configuration -> configuration.dependencies.withType(ProjectDependency::class.java) }
            .map { it.path }
            .distinct()
            .sorted()

        task.configure { dependencies.set(declared) }
    }

    tasks.matching { it.name == "preBuild" || it.name == "compileKotlin" }
        .configureEach { dependsOn(task) }
}

private val DEPENDENCY_CONFIGURATIONS = listOf("implementation", "api", "compileOnly")

internal enum class ModuleTier {
    CORE,
    DATA,
    FEATURE,
    APP,
    ;

    /** Prefixes this tier is allowed to depend on. */
    fun permits(): List<String> = when (this) {
        CORE -> listOf(":core:")
        DATA -> listOf(":core:")
        FEATURE -> listOf(":core:", ":data:")
        APP -> listOf(":core:", ":data:", ":feature:")
    }

    companion object {
        fun of(path: String): ModuleTier? = when {
            path.startsWith(":core:") -> CORE
            path.startsWith(":data:") -> DATA
            path.startsWith(":feature:") -> FEATURE
            else -> null
        }
    }
}

internal abstract class VerifyModuleDependenciesTask : DefaultTask() {

    @get:Input
    abstract val modulePath: Property<String>

    @get:Input
    abstract val tierName: Property<String>

    @get:Input
    abstract val dependencies: ListProperty<String>

    @TaskAction
    fun verify() {
        val tier = ModuleTier.valueOf(tierName.get())
        val allowed = tier.permits()
        val self = modulePath.get()

        val violations = dependencies.get().filter { dependency ->
            dependency != self && allowed.none { dependency.startsWith(it) }
        }

        if (violations.isEmpty()) return

        throw GradleException(
            """
            $self is a ${tier.name.lowercase()} module and may only depend on
            ${allowed.joinToString()} — but it declares:

            ${violations.joinToString("\n            ") { "  $it" }}

            ${explain(tier)}
            """.trimIndent(),
        )
    }

    private fun explain(tier: ModuleTier): String = when (tier) {
        ModuleTier.FEATURE ->
            "Two features that need the same data should both depend on the :data: module that " +
                "owns it. If they need to react to each other, expose that state as a singleton " +
                "from :data: rather than wiring one screen to another."
        ModuleTier.DATA ->
            "A data module reaching for a sibling means the domain boundary is in the wrong " +
                "place. Move the shared type down into :core:model, or merge the two domains."
        ModuleTier.CORE ->
            "Core is the bottom of the graph. If it needs something from a data or feature " +
                "module, invert it: declare the interface in core and bind the implementation " +
                "from the module that has it."
        ModuleTier.APP -> ""
    }
}
