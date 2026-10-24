package com.base.app.buildlogic.guard

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

/**
 * Enforces the module graph per module at build time:
 *
 * ```
 *   :androidApp, :shared    → anything
 *   :feature:x              → :core:*, :data:*      (never another :feature:*)
 *   :data:x                 → :core:*               (never another :data:*)
 *   :core:x                 → :core:*
 * ```
 *
 * `:core:common` is pure Kotlin, and applying an Android or Compose plugin to it fails the same
 * check.
 *
 * The sibling rules are the load-bearing ones. A single `:feature:cart → :feature:catalog` edge is
 * harmless on the day it is added and, eighteen months later, is the reason nothing can be built or
 * tested in isolation and every change recompiles everything. It always arrives as a one-line
 * convenience in a pull request that is about something else, which is why this is a build failure
 * rather than a review checklist item.
 */
internal fun Project.verifyModuleDependencies() {
    val projectPath = path
    if (ModuleShape.of(projectPath) == null) return

    val task = tasks.register<VerifyModuleDependenciesTask>("verifyModuleDependencies") {
        group = "verification"
        description = "Fails if this module depends on one it is not allowed to see."
        modulePath.set(projectPath)
        // Resolved lazily: the convention plugins apply Android and Compose after this one runs.
        platformPlugins.set(provider { PLATFORM_PLUGINS.filter(pluginManager::hasPlugin) })
        marker.set(layout.buildDirectory.file("guards/verifyModuleDependencies.ok"))
    }

    afterEvaluate {
        val declared = configurations
            .filter { configuration -> CHECKED_CONFIGURATIONS.any { configuration.name.endsWith(it, ignoreCase = true) } }
            .flatMap { configuration -> configuration.dependencies.withType(ProjectDependency::class.java) }
            .map { it.path }
            .distinct()
            .sorted()

        task.configure { dependencies.set(declared) }
    }

    hookIntoBuild(task.name)
}

/** Runs [taskName] before every Kotlin compilation of the module, and with `check`. */
internal fun Project.hookIntoBuild(taskName: String) {
    tasks.withType(KotlinCompilationTask::class.java).configureEach { dependsOn(taskName) }
    tasks.named { it == "check" }.configureEach { dependsOn(taskName) }
}

/**
 * Every source set's `implementation`, `api`, `compileOnly` and `runtimeOnly`:
 * `commonMainImplementation`, `iosMainApi`, `androidHostTestImplementation` and the rest.
 */
private val CHECKED_CONFIGURATIONS = listOf("api", "implementation", "compileOnly", "runtimeOnly")

private val PLATFORM_PLUGINS = listOf(
    "com.android.application",
    "com.android.kotlin.multiplatform.library",
    "org.jetbrains.kotlin.plugin.compose",
    "org.jetbrains.compose",
)

internal enum class ModuleKind(val pure: Boolean) {
    CORE(pure = false),
    CORE_COMMON(pure = true),
    FEATURE(pure = false),
    DATA(pure = false),
}

/** Where a module sits in the graph, recognised by its path. */
internal data class ModuleShape(val kind: ModuleKind) {

    /** The dependencies this module may not have, as `from -> to` edges. */
    fun violations(self: String, dependencies: List<String>): List<String> =
        dependencies.filter { it != self && !allows(it) }.map { "$self -> $it" }

    private fun allows(dependency: String): Boolean {
        val other = of(dependency)?.kind
        return when (kind) {
            ModuleKind.CORE, ModuleKind.CORE_COMMON -> other.isCore()
            ModuleKind.FEATURE -> other.isCore() || other == ModuleKind.DATA
            ModuleKind.DATA -> other.isCore()
        }
    }

    fun rule(): String = when (kind) {
        ModuleKind.CORE, ModuleKind.CORE_COMMON ->
            "A core module may depend only on other :core: modules. If it needs something from a " +
                "data or feature module, invert it: declare the interface in core and bind the " +
                "implementation from the module that has it."
        ModuleKind.FEATURE ->
            "A feature may depend on :core:* and :data:*, never on another feature. A screen another " +
                "feature opens gets its key in core/navigation/src/commonMain/kotlin/<package path>/" +
                "feature/<owner>/api/, in the owner's api package, where both features can reach it."
        ModuleKind.DATA ->
            "A data module may depend only on :core:*. A rule that needs two domains belongs in the " +
                "feature that combines them, or the two domains are one."
    }

    companion object {
        fun of(path: String): ModuleShape? {
            val parts = path.removePrefix(":").split(':')
            val kind = when {
                path == ":core:common" -> ModuleKind.CORE_COMMON
                parts[0] == "core" && parts.size >= 2 -> ModuleKind.CORE
                parts[0] == "feature" && parts.size == 2 -> ModuleKind.FEATURE
                parts[0] == "data" && parts.size == 2 -> ModuleKind.DATA
                else -> return null
            }
            return ModuleShape(kind)
        }
    }
}

private fun ModuleKind?.isCore() = this == ModuleKind.CORE || this == ModuleKind.CORE_COMMON

@CacheableTask
internal abstract class VerifyModuleDependenciesTask : DefaultTask() {

    @get:Input
    abstract val modulePath: Property<String>

    @get:Input
    abstract val dependencies: ListProperty<String>

    /** The Android or Compose plugins this module applies, which a pure module may not. */
    @get:Input
    abstract val platformPlugins: ListProperty<String>

    @get:OutputFile
    abstract val marker: RegularFileProperty

    @TaskAction
    fun verify() {
        val self = modulePath.get()
        val shape = checkNotNull(ModuleShape.of(self))

        val plugins = platformPlugins.get()
        if (shape.kind.pure && plugins.isNotEmpty()) {
            throw GradleException(
                "$self is a pure Kotlin module and may not apply ${plugins.joinToString()}. Keep it on " +
                    "com.base.app.kmp.pure; code that needs Android, iOS or Compose belongs in another " +
                    ":core: module.",
            )
        }

        val violations = shape.violations(self, dependencies.get())
        if (violations.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("Module graph rule broken:")
                    appendLine()
                    violations.forEach { appendLine("  $it") }
                    appendLine()
                    append(shape.rule())
                },
            )
        }

        marker.get().asFile.writeText("ok")
    }
}
