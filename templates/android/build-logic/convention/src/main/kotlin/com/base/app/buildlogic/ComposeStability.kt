package com.base.app.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension
import java.io.File

/** Reads the Compose compiler's own stability report and fails on a new recomposition cost. */
internal fun Project.configureComposeStability() {
    if (path !in STABILITY_CHECKED_MODULES) return

    // Kept here so the whole stability feature is in one file.
    extensions.configure<ComposeCompilerGradlePluginExtension> {
        // reportsDestination names each composable; metricsDestination only gives module totals.
        reportsDestination.set(layout.buildDirectory.dir("compose_reports"))
    }

    // A file tree rather than the directory itself: the compiler has not written it at
    // configuration time, and Gradle rejects an @InputDirectory that does not exist.
    val reportsDir = layout.buildDirectory.dir("compose_reports")

    /*
     * Declared as an output of the compilation that writes it.
     */
    tasks.matching { it.name == COMPILE_TASK }.configureEach {
        outputs.dir(reportsDir).withPropertyName("composeStabilityReports")
    }

    val written = fileTree(reportsDir) {
        include("**/*-composables.txt")
    }
    val baseline = rootProject.layout.projectDirectory
        .file("config/compose-stability-baseline.txt")

    val check = tasks.register<CheckComposeStabilityTask>("checkComposeStability") {
        group = "verification"
        description = "Fails on a composable that recomposes when it did not need to."
        reports.setFrom(written)
        baselineFile.set(baseline)
        modulePath.set(path)
        // The report is a by-product of compiling, so the task that writes it has to have run.
        dependsOn(COMPILE_TASK)
    }

    tasks.register<RecordComposeStabilityTask>("recordComposeStability") {
        group = "verification"
        description = "Writes the current unskippable composables to the baseline."
        reports.setFrom(written)
        baselineFile.set(baseline)
        dependsOn(COMPILE_TASK)
    }

    // Runs with the rest of the verification, rather than on preBuild like the other guards:
    // there is nothing to read until the module has been compiled.
    tasks.matching { it.name == "check" }.configureEach { dependsOn(check) }
}

/** The modules whose components every screen calls, and therefore the only ones worth the cost. */
private val STABILITY_CHECKED_MODULES = setOf(":core:designsystem", ":core:ui")

/**
 * The compilation that writes the report. One variant, not all of them: these are library modules
 * with no flavours, and the two build types compile the same sources to the same stability answers.
 */
private const val COMPILE_TASK = "compileDebugKotlin"

/**
 * Every composable the report says is paying for a recomposition it did not need, as
 * `FunctionName`.
 *
 * * **restartable but not skippable.** The function re-executes whenever its parent recomposes,
 *   unconditionally. In a list row that is once per row per frame.
 * * **an unstable parameter.** Strong skipping keeps the function skippable, but by comparing that
 *   parameter with `equals` on every recomposition instead of skipping the comparison — which for
 *   a `List` is O(n) per frame, and for a `var`-holding class is a comparison that never says
 *   equal.
 */
private fun offenders(reports: Set<File>): List<String> =
    reports.asSequence()
        .filter { it.isFile }
        .flatMap { report ->
            var current: String? = null
            report.readLines().mapNotNull { line ->
                val declaration = FUNCTION.find(line)
                if (declaration != null) {
                    current = declaration.groupValues[1]
                    val unskippable = line.startsWith("restartable ") && !line.contains(" skippable ")
                    return@mapNotNull if (unskippable) current else null
                }
                // A parameter line, which the compiler indents under the function it belongs to.
                if (line.trimStart().startsWith("unstable ")) current else null
            }
        }
        .filterNotNull()
        .distinct()
        .sorted()
        .toList()

/** `fun com.base.app.core.designsystem.component.AppButton(` → `AppButton`. */
private val FUNCTION = Regex("\\bfun\\s+(?:[\\w.]+\\.)?(\\w+)\\s*\\(")

internal abstract class CheckComposeStabilityTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val reports: ConfigurableFileCollection

    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val baselineFile: RegularFileProperty

    @get:Input
    abstract val modulePath: Property<String>

    @TaskAction
    fun verify() {
        val allowed = baselineFile.asFile.orNull
            ?.takeIf { it.isFile }
            ?.readLines()
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() && !it.startsWith("#") }
            .orEmpty()
            .toSet()

        val present = reports.files.filter { it.isFile }
        if (present.isEmpty()) {
            throw GradleException(
                "${modulePath.get()}: the Compose compiler wrote no stability report, so this " +
                    "check has nothing to read. That is a build configuration problem rather than " +
                    "a clean result — silence here would be indistinguishable from passing. " +
                    "Check that reportsDestination is still set in ComposeStability.kt.",
            )
        }

        val found = offenders(present.toSet())
        val fresh = found - allowed

        if (fresh.isEmpty()) return

        throw GradleException(
            """
            ${modulePath.get()} has ${fresh.size} composable(s) taking a parameter the Compose
            compiler cannot prove is immutable. That makes the whole composable unskippable: it
            re-runs on every recomposition of whatever called it — in a list, once per row per
            frame:

            ${fresh.joinToString("\n            ") { "  $it" }}

            Read build/compose_reports/*-composables.txt for the parameter that caused it. It is
            almost always one of three things: a List where a kotlinx.collections.immutable
            PersistentList would do, a type from a module without the Compose compiler plugin, or
            a `var` in a class being passed as a parameter. A type this project cannot change —
            one from a library — goes in config/compose-stability.conf instead.

            If it genuinely has to stay, run :${modulePath.get().removePrefix(":")}:recordComposeStability
            to accept it, in a commit that says why.
            """.trimIndent(),
        )
    }
}

internal abstract class RecordComposeStabilityTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val reports: ConfigurableFileCollection

    @get:OutputFile
    abstract val baselineFile: RegularFileProperty

    @TaskAction
    fun record() {
        val file = baselineFile.get().asFile
        val existing = file.takeIf { it.isFile }
            ?.readLines()
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() && !it.startsWith("#") }
            .orEmpty()

        // Merged rather than replaced: each module writes its own report, so recording from one
        // module would otherwise delete every other module's accepted entries.
        val merged = (existing + offenders(reports.files)).distinct().sorted()

        file.parentFile.mkdirs()
        file.writeText(
            buildString {
                appendLine("# Composables paying for a recomposition they did not need, accepted for now.")
                appendLine("#")
                appendLine("# Written by :recordComposeStability. Deleting a line is how a fix is")
                appendLine("# recorded — the check fails again if it comes back.")
                merged.forEach { appendLine(it) }
            },
        )
        logger.lifecycle("Recorded ${merged.size} composable(s) in ${file.path}")
    }
}
