package com.base.app.buildlogic.guard

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.register

/**
 * Fails when a file in an `api` package (or a shared-key folder in `:core:navigation`, which uses the
 * owner's `api` package) imports something an `api` package must not see. Every source set counts:
 * an `api` package is the surface other modules compile against on both platforms.
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
                    appendLine("An api package imports what it must not see:")
                    appendLine()
                    violations.forEach { appendLine("  $it") }
                    appendLine()
                    append(
                        "Files in an api package hold keys, interfaces and models: pure Kotlin that may " +
                            "use :core:common and its own api package, and nothing from Android, iOS, " +
                            "Compose, Koin or another module. Move the import's user out of the api package.",
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
    private val FORBIDDEN = listOf("android.", "androidx.compose.", "org.jetbrains.compose.", "org.koin.", "platform.")

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
