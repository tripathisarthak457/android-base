package com.example.finalafter.crui3.buildlogic.guard

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
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

/**
 * Enforces the module graph per module at build time, for both project shapes: compact (one module
 * per feature and per data domain) and modular (each split into `:api` and `:impl`).
 *
 * ```
 *   :app, :catalog          → anything
 *   :feature:x              → :core:*, :data:*                                   (never another :feature:*)
 *   :feature:x:api          → :core:common
 *   :feature:x:impl         → :core:*, :data:*:api, :domain:*, :feature:*:api      (never an :impl)
 *   :data:x                 → :core:*                                            (never another :data:*)
 *   :data:x:api             → :core:common
 *   :data:x:impl            → :data:x:api, :core:*
 *   :domain:x               → :data:*:api, :core:common
 *   :core:designsystem      → :core:testing:*, and only Compose and the libraries listed below
 *   :core:navigation        → :core:*, except the shell built on it
 *   :core:x                 → :core:*
 * ```
 *
 * Only `:app` and the catalog compose `:core:navigation:shell`. `:core:common`, every `:api` module
 * and every `:domain:` module are pure Kotlin, and applying an Android or Compose plugin to one fails
 * the same check.
 *
 * The sibling rules are the load-bearing ones. A single `:feature:cart → :feature:catalog` edge is
 * harmless on the day it is added and, eighteen months later, is the reason nothing can be built or
 * tested in isolation and every change recompiles everything. It always arrives as a one-line
 * convenience in a pull request that is about something else, which is why this is a build failure
 * rather than a review checklist item.
 */
internal fun Project.verifyModuleDependencies() {
    val projectPath = path
    val shape = ModuleShape.of(projectPath) ?: return

    // Read when the task's inputs are, once every build file and plugin has declared its
    // dependencies, rather than in an afterEvaluate that holds the project open for it.
    val declared = provider {
        configurations
            .filter { configuration -> CHECKED_CONFIGURATIONS.any { configuration.name.endsWith(it, ignoreCase = true) } }
            .flatMap { configuration -> configuration.dependencies }
    }

    val task = tasks.register<VerifyModuleDependenciesTask>("verifyModuleDependencies") {
        group = "verification"
        description = "Fails if this module depends on one it is not allowed to see."
        modulePath.set(projectPath)
        dependencies.set(
            declared.map { all -> all.filterIsInstance<ProjectDependency>().map { it.path }.distinct().sorted() },
        )
        libraries.set(
            declared.map { all ->
                if (shape.libraries == null) {
                    emptyList()
                } else {
                    all.filterIsInstance<ExternalModuleDependency>().map { "${it.group}:${it.name}" }.distinct().sorted()
                }
            },
        )
        // Resolved lazily: the convention plugins apply Compose after this one runs.
        androidPlugins.set(provider { ANDROID_PLUGINS.filter(pluginManager::hasPlugin) })
        marker.set(layout.buildDirectory.file("guards/verifyModuleDependencies.ok"))
    }

    hookIntoBuild(task.name)
}

/**
 * Fails when a file in an `api` package (or a shared-key folder in `:core:navigation`, which uses the
 * owner's `api` package) imports something an `:api` module cannot see. The compact shape compiles
 * such an import happily; the modular shape, where each `api` package is its own pure module,
 * would not, so the guard keeps every compact project splittable.
 */
internal fun Project.verifyApiPurity() {
    val task = tasks.register<VerifyApiPurityTask>("verifyApiPurity") {
        group = "verification"
        description = "Fails if an api package imports Android, Compose, DI or another module's internals."
        sources.from(fileTree("src/main") { include("**/*.kt") })
        projectDirectory.set(layout.projectDirectory)
        marker.set(layout.buildDirectory.file("guards/verifyApiPurity.ok"))
    }
    hookIntoBuild(task.name)
}

// By name, so only these tasks are realized rather than every task in the module.
private fun Project.hookIntoBuild(taskName: String) {
    tasks.named { it in HOOKED_TASKS }.configureEach { dependsOn(taskName) }
}

private val HOOKED_TASKS = setOf("preBuild", "compileKotlin", "check")

/** `implementation`, `api`, `compileOnly`, `runtimeOnly`, and every variant and test form of them. */
private val CHECKED_CONFIGURATIONS = listOf("api", "implementation", "compileOnly", "runtimeOnly")

private val ANDROID_PLUGINS = listOf(
    "com.android.application",
    "com.android.library",
    "com.android.kotlin.multiplatform.library",
    "org.jetbrains.kotlin.plugin.compose",
)

internal enum class ModuleKind(val pure: Boolean) {
    CORE(pure = false),
    CORE_COMMON(pure = true),
    DESIGN_SYSTEM(pure = false),
    NAVIGATION(pure = false),
    FEATURE(pure = false),
    FEATURE_API(pure = true),
    FEATURE_IMPL(pure = false),
    DATA(pure = false),
    DATA_API(pure = true),
    DATA_IMPL(pure = false),
    DOMAIN(pure = true),
}

/** Where a module sits in the graph, recognised by its path. [name] is the `x` of `:data:x:impl`. */
internal data class ModuleShape(val kind: ModuleKind, val name: String) {

    /** The libraries this module may use, as `group:name` globs, or null when they are not checked. */
    val libraries: List<String>? get() = if (kind == ModuleKind.DESIGN_SYSTEM) DESIGN_SYSTEM_LIBRARIES else null

    /** The dependencies this module may not have, as `from -> to` edges. */
    fun violations(self: String, dependencies: List<String>): List<String> =
        dependencies.filter { it != self && !allows(it) }.map { "$self -> $it" }

    /** The libraries, as `group:name`, this module may not use, as `from -> to` edges. */
    fun libraryViolations(self: String, libraries: List<String>): List<String> {
        val allowed = this.libraries?.map(::glob) ?: return emptyList()
        return libraries.filter { library -> allowed.none { it.matches(library) } }.map { "$self -> $it" }
    }

    private fun allows(dependency: String): Boolean {
        val other = of(dependency)?.kind
        if (dependency == SHELL && kind in SHELL_BLIND) return false
        return when (kind) {
            ModuleKind.CORE, ModuleKind.CORE_COMMON -> other.isCore()
            ModuleKind.DESIGN_SYSTEM -> dependency == TESTING || dependency.startsWith("$TESTING:")
            ModuleKind.NAVIGATION -> other.isCore() && dependency !in NAVIGATION_HIDDEN
            ModuleKind.FEATURE -> other.isCore() || other == ModuleKind.DATA
            ModuleKind.FEATURE_API, ModuleKind.DATA_API -> other == ModuleKind.CORE_COMMON
            ModuleKind.FEATURE_IMPL -> other.isCore() ||
                other in setOf(ModuleKind.DATA_API, ModuleKind.DOMAIN, ModuleKind.FEATURE_API)
            ModuleKind.DATA -> other.isCore()
            ModuleKind.DATA_IMPL -> other.isCore() || dependency == ":data:$name:api"
            ModuleKind.DOMAIN -> other == ModuleKind.CORE_COMMON || other == ModuleKind.DATA_API
        }
    }

    fun rule(): String = when (kind) {
        ModuleKind.CORE, ModuleKind.CORE_COMMON ->
            "A core module may depend only on other :core: modules. If it needs something from a " +
                "data or feature module, invert it: declare the interface in core and bind the " +
                "implementation from the module that has it."
        ModuleKind.DESIGN_SYSTEM ->
            "The design system may depend on no other module but :core:testing in its tests, and on " +
                "no library but Compose and the few build-logic's ModuleGraph.kt lists: every screen " +
                "uses it, and a component that cannot reach data, DI or the network cannot fetch, " +
                "inject or hold state behind its caller's back. Take values and callbacks instead."
        ModuleKind.NAVIGATION ->
            "The navigation runtime may depend on other :core: modules, never on :core:designsystem or " +
                ":core:navigation:shell: every feature sees the runtime, so what it sees they see. Drawing " +
                "belongs in the shell."
        ModuleKind.FEATURE ->
            "A feature may depend on :core:* and :data:*, never on another feature or on " +
                ":core:navigation:shell. A screen another feature opens gets its key in " +
                "core/navigation/src/main/kotlin/<package path>/feature/<owner>/api/, in the owner's " +
                "api package, where both features can reach it."
        ModuleKind.FEATURE_API, ModuleKind.DATA_API ->
            "An :api module holds keys, interfaces and models, and may depend only on :core:common."
        ModuleKind.FEATURE_IMPL ->
            "A feature's :impl may depend on :core:*, :data:*:api, :domain:* and :feature:*:api, " +
                "never on another :impl."
        ModuleKind.DATA ->
            "A data module may depend only on :core:*. A rule that needs two domains belongs in a " +
                ":domain: module, or the two domains are one."
        ModuleKind.DATA_IMPL ->
            "A data :impl may depend on its own :api and :core:*. A rule that needs another domain " +
                "belongs in a :domain: module."
        ModuleKind.DOMAIN ->
            "A domain module is pure Kotlin over :data:*:api and :core:common."
    }

    companion object {
        fun of(path: String): ModuleShape? {
            val parts = path.removePrefix(":").split(':')
            val name = parts.getOrElse(1) { "" }
            val kind = when {
                path == ":core:common" -> ModuleKind.CORE_COMMON
                path == DESIGN_SYSTEM -> ModuleKind.DESIGN_SYSTEM
                path == NAVIGATION -> ModuleKind.NAVIGATION
                parts[0] == "core" && parts.size >= 2 -> ModuleKind.CORE
                parts.size == 2 && parts[0] == "feature" -> ModuleKind.FEATURE
                parts.size == 3 && parts[0] == "feature" && parts[2] == "api" -> ModuleKind.FEATURE_API
                parts.size == 3 && parts[0] == "feature" && parts[2] == "impl" -> ModuleKind.FEATURE_IMPL
                parts.size == 2 && parts[0] == "data" -> ModuleKind.DATA
                parts.size == 3 && parts[0] == "data" && parts[2] == "api" -> ModuleKind.DATA_API
                parts.size == 3 && parts[0] == "data" && parts[2] == "impl" -> ModuleKind.DATA_IMPL
                parts.size == 2 && parts[0] == "domain" -> ModuleKind.DOMAIN
                else -> return null
            }
            return ModuleShape(kind, name)
        }
    }
}

private fun ModuleKind?.isCore() =
    this == ModuleKind.CORE || this == ModuleKind.CORE_COMMON || this == ModuleKind.DESIGN_SYSTEM ||
        this == ModuleKind.NAVIGATION

private const val DESIGN_SYSTEM = ":core:designsystem"
private const val NAVIGATION = ":core:navigation"
private const val SHELL = ":core:navigation:shell"
private const val TESTING = ":core:testing"

/** What the navigation runtime may not see: every feature sees the runtime, so it would see this too. */
private val NAVIGATION_HIDDEN = setOf(SHELL, DESIGN_SYSTEM)

/** Modules that never compose the shell: it is the app's, assembled in :app and the catalog. */
private val SHELL_BLIND = setOf(
    ModuleKind.FEATURE, ModuleKind.FEATURE_API, ModuleKind.FEATURE_IMPL,
    ModuleKind.DATA, ModuleKind.DATA_API, ModuleKind.DATA_IMPL, ModuleKind.DOMAIN,
)

/**
 * The libraries `:core:designsystem` may declare, in any configuration, its tests' included.
 * Compose is the design system's whole toolkit; each other line is a reason, not a convenience.
 */
private val DESIGN_SYSTEM_LIBRARIES = listOf(
    // Compose without Material, from its BOM, with its tooling and test rule, Google Fonts among them:
    // the design system draws its own components.
    "androidx.compose:compose-bom",
    "androidx.compose.runtime:*",
    "androidx.compose.ui:*",
    "androidx.compose.foundation:*",
    "androidx.compose.animation:*",
    // Android's own window size classes, so breakpoints match every other app on the device.
    "androidx.window:*",
    // Back and predictive back for sheets and overlays, and the lifecycle a timed message counts against.
    "androidx.navigationevent:*",
    "androidx.lifecycle:lifecycle-runtime-compose",
    // A test activity that owns a back dispatcher, as every app's activity does.
    "androidx.activity:activity-compose",
    // Liquid glass on floating chrome, and its continuous corners.
    "io.github.kyant0:*",
    // Added by the Kotlin plugin.
    "org.jetbrains.kotlin:kotlin-stdlib*",
    // The tests the screenshots convention generates from previews, with Google's accessibility
    // checks, and the device-test runner the conventions add to a module that has device tests.
    "junit:junit",
    "org.robolectric:*",
    "io.github.takahirom.roborazzi:*",
    "io.github.sergio-sastre.ComposablePreviewScanner:*",
    "com.google.android.apps.common.testing.accessibility.framework:*",
    "androidx.test*:*",
)

/** `androidx.compose.ui:*` as a regex over `group:name`, where `*` is any run of characters. */
private fun glob(pattern: String): Regex =
    Regex(pattern.split('*').joinToString(".*") { Regex.escape(it) })

@CacheableTask
internal abstract class VerifyModuleDependenciesTask : DefaultTask() {

    @get:Input
    abstract val modulePath: Property<String>

    @get:Input
    abstract val dependencies: ListProperty<String>

    @get:Input
    abstract val libraries: ListProperty<String>

    /** The Android or Compose plugins this module applies, which a pure module may not. */
    @get:Input
    abstract val androidPlugins: ListProperty<String>

    @get:OutputFile
    abstract val marker: RegularFileProperty

    @TaskAction
    fun verify() {
        val self = modulePath.get()
        val shape = checkNotNull(ModuleShape.of(self))

        val plugins = androidPlugins.get()
        if (shape.kind.pure && plugins.isNotEmpty()) {
            throw GradleException(
                "$self is a pure Kotlin module and may not apply ${plugins.joinToString()}. Apply " +
                    "com.example.finalafter.crui3.jvm.library instead; Android code belongs in an :impl or :core module.",
            )
        }

        val violations = shape.violations(self, dependencies.get()) + shape.libraryViolations(self, libraries.get())
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
                        "Files in an api package hold keys, interfaces and models: pure Kotlin that may " +
                            "use :core:common and its own api package, and nothing from Android, Compose, " +
                            "DI or another module. Move the import's user out of the api package.",
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

    private val PACKAGE = Regex("""^package\s+([\w.]+)""", RegexOption.MULTILINE)
    private val API_PACKAGE = Regex("""^(.+)\.(feature|data)\.(\w+)\.api(\..+)?$""")
    private val IMPORT = Regex("""^import\s+([\w.]+)""")
    private val FORBIDDEN = listOf("android.", "androidx.compose.", "dagger.", "javax.inject.")

    /** The imports in [source] that an api package may not have. Empty for a file in any other package. */
    fun violations(source: String): List<Violation> {
        val pkg = PACKAGE.find(source)?.groupValues?.get(1) ?: return emptyList()
        val api = API_PACKAGE.matchEntire(pkg) ?: return emptyList()
        val root = api.groupValues[1]
        val allowed = listOf("$root.core.common.", "$root.${api.groupValues[2]}.${api.groupValues[3]}.api.")

        return source.lines().mapIndexedNotNull { index, line ->
            val imported = IMPORT.find(line.trim())?.groupValues?.get(1) ?: return@mapIndexedNotNull null
            val forbidden = FORBIDDEN.any(imported::startsWith) ||
                (imported.startsWith("$root.") && allowed.none(imported::startsWith))
            if (forbidden) Violation(index + 1, imported) else null
        }
    }
}
