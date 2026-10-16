package com.base.app.buildlogic.guard

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

/**
 * Enforces the module graph per module at build time:
 *
 * ```
 *   :androidApp, :shared    → anything
 *   :feature:x              → :core:*, :data:*          (never another :feature:*)
 *   :data:x                 → :core:*                   (never another :data:*)
 *   :core:x                 → :core:*
 * ```
 *
 * `:core:common` is pure Kotlin, and applying an Android or Compose plugin to it fails the same
 * check: it is the one module both templates share file for file.
 *
 * The sibling rules are the load-bearing ones. A single `:feature:cart → :feature:catalog` edge is
 * harmless on the day it is added and, a year later, is the reason nothing can be built or tested
 * in isolation. It always arrives as a one-line convenience in a pull request about something
 * else, which is why this is a build failure rather than a review checklist item.
 */
internal fun Project.verifyModuleDependencies() {
    val projectPath = path
    if (ModuleShape.of(projectPath) == null) return

    val task = tasks.register<VerifyModuleDependenciesTask>("verifyModuleDependencies") {
        group = "verification"
        description = "Fails if this module depends on one it is not allowed to see."
        modulePath.set(projectPath)
        // Resolved lazily: the convention plugins apply Compose after this one runs.
        platformPlugins.set(provider { PLATFORM_PLUGINS.filter(pluginManager::hasPlugin) })
        marker.set(layout.buildDirectory.file("guards/verifyModuleDependencies.ok"))
    }

    afterEvaluate {
        val declared = configurations
            .filter { configuration ->
                CHECKED_CONFIGURATIONS.any { configuration.name.endsWith(it, ignoreCase = true) }
            }
            .flatMap { it.dependencies.withType(ProjectDependency::class.java) }
            .map { it.path }
            .distinct()
            .sorted()

        task.configure { dependencies.set(declared) }
    }

    hookIntoBuild(task.name)
}

/**
 * Fails when a file in an `api` package imports something an `:api` module could not see. Keeps
 * every compact module splittable into `:api` and `:impl` later without touching an import.
 */
internal fun Project.verifyApiPurity() {
    val task = tasks.register<VerifyApiPurityTask>("verifyApiPurity") {
        group = "verification"
        description = "Fails if an api package imports Android, Compose, DI or another module's internals."
        sources.from(fileTree("src") { include("*Main/kotlin/**/*.kt") })
        projectDirectory.set(layout.projectDirectory)
        marker.set(layout.buildDirectory.file("guards/verifyApiPurity.ok"))
    }
    hookIntoBuild(task.name)
}

private fun Project.hookIntoBuild(taskName: String) {
    tasks.withType(KotlinCompilationTask::class.java).configureEach { dependsOn(taskName) }
    tasks.named { it == "check" }.configureEach { dependsOn(taskName) }
}

/** `implementation`, `api`, `compileOnly`, `runtimeOnly`, in every source set and test form. */
private val CHECKED_CONFIGURATIONS = listOf("api", "implementation", "compileOnly", "runtimeOnly")

private val PLATFORM_PLUGINS = listOf(
    "com.android.application",
    "com.android.kotlin.multiplatform.library",
    "org.jetbrains.kotlin.plugin.compose",
    "org.jetbrains.compose",
)

internal enum class ModuleKind(val pure: Boolean) {
    Core(pure = false),
    CoreCommon(pure = true),
    Feature(pure = false),
    Data(pure = false),
}

/** Where a module sits in the graph, recognised by its path. */
internal data class ModuleShape(val kind: ModuleKind) {

    /** The dependencies this module may not have, as `from -> to` edges. */
    fun violations(self: String, dependencies: List<String>): List<String> =
        dependencies.filter { it != self && !allows(it) }.map { "$self -> $it" }

    private fun allows(dependency: String): Boolean {
        val other = of(dependency)?.kind
        return when (kind) {
            ModuleKind.Core, ModuleKind.CoreCommon, ModuleKind.Data -> other.isCore()
            ModuleKind.Feature -> other.isCore() || other == ModuleKind.Data
        }
    }

    fun rule(): String = when (kind) {
        ModuleKind.Core, ModuleKind.CoreCommon ->
            "A core module may depend only on other :core: modules. If it needs something from a " +
                "data or feature module, invert it: declare the interface in core and bind the " +
                "implementation from the module that has it."
        ModuleKind.Feature ->
            "A feature may depend on :core:* and :data:*, never on another feature. A screen " +
                "another feature opens gets its key in core/navigation/src/commonMain/kotlin/" +
                "<package path>/feature/<owner>/api/, in the owner's api package, where both " +
                "features can reach it."
        ModuleKind.Data ->
            "A data module may depend only on :core:*. A rule that needs two domains belongs in " +
                "the feature that combines them, or the two domains are one."
    }

    companion object {
        fun of(path: String): ModuleShape? {
            val parts = path.removePrefix(":").split(':')
            val kind = when {
                path == ":core:common" -> ModuleKind.CoreCommon
                parts.size == 2 && parts[0] == "core" -> ModuleKind.Core
                parts.size == 2 && parts[0] == "feature" -> ModuleKind.Feature
                parts.size == 2 && parts[0] == "data" -> ModuleKind.Data
                else -> return null
            }
            return ModuleShape(kind)
        }
    }
}

private fun ModuleKind?.isCore() = this == ModuleKind.Core || this == ModuleKind.CoreCommon

@CacheableTask
internal abstract class VerifyModuleDependenciesTask : DefaultTask() {

    @get:Input
    abstract val modulePath: Property<String>

    @get:Input
    abstract val dependencies: ListProperty<String>

    /** The Android and Compose plugins this module applies, which a pure module may not. */
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
                "$self is a pure Kotlin module and may not apply ${plugins.joinToString()}. It " +
                    "applies com.base.app.kmp.pure, so it compiles unchanged for every target.",
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

@CacheableTask
internal abstract class VerifyApiPurityTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    /** Only names the files in the failure. Not an input, so the cache key stays machine-independent. */
    @get:Internal
    abstract val projectDirectory: DirectoryProperty

    @get:OutputFile
    abstract val marker: RegularFileProperty

    @TaskAction
    fun verify() {
        val base = projectDirectory.get().asFile
        val violations = sources.files
            .filter { it.isFile }
            .sortedBy { it.path }
            .flatMap { file ->
                val name = file.relativeTo(base).invariantSeparatorsPath
                ApiPurity.violations(file.readText()).map { "$name:${it.line}: ${it.import}" }
            }

        if (violations.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("An api package imports what an :api module cannot see:")
                    appendLine()
                    violations.forEach { appendLine("  $it") }
                    appendLine()
                    append(
                        "Files in an api package hold keys, interfaces and models: pure Kotlin " +
                            "that may use :core:common and its own api package, and nothing from " +
                            "Android, Compose, DI or another module. Move the import's user out " +
                            "of the api package.",
                    )
                },
            )
        }

        marker.get().asFile.writeText("ok")
    }
}

/** The import rules for files in an `api` package, apart from Gradle so they can be unit tested. */
internal object ApiPurity {

    data class Violation(val line: Int, val import: String)

    private val Package = Regex("""^package\s+([\w.]+)""", RegexOption.MULTILINE)
    private val ApiPackage = Regex("""^(.+)\.(feature|data)\.(\w+)\.api(\..+)?$""")
    private val Import = Regex("""^import\s+([\w.]+)""")
    private val Forbidden = listOf(
        "android.",
        "androidx.compose.",
        "org.jetbrains.compose.",
        "org.koin.",
        "platform.",
    )

    /** The imports in [source] that an api package may not have. Empty for any other package. */
    fun violations(source: String): List<Violation> {
        val pkg = Package.find(source)?.groupValues?.get(1) ?: return emptyList()
        val api = ApiPackage.matchEntire(pkg) ?: return emptyList()
        val root = api.groupValues[1]
        val allowed = listOf(
            "$root.core.common.",
            "$root.${api.groupValues[2]}.${api.groupValues[3]}.api.",
        )

        return source.lines().mapIndexedNotNull { index, line ->
            val imported = Import.find(line.trim())?.groupValues?.get(1)
                ?: return@mapIndexedNotNull null
            val forbidden = Forbidden.any(imported::startsWith) ||
                (imported.startsWith("$root.") && allowed.none(imported::startsWith))
            if (forbidden) Violation(index + 1, imported) else null
        }
    }
}
