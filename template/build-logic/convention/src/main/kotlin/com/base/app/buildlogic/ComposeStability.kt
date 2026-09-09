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

/**
 * Reads the Compose compiler's own stability report and fails on a new recomposition cost.
 *
 * The report has always been available — `enableComposeCompilerMetrics` has been in
 * `gradle.properties` since the first commit — and reading it is a thing somebody does once,
 * during a performance investigation, six months after the composable that caused the problem
 * was merged. This makes it a build failure on the day it is written instead.
 *
 * ## What is being checked
 *
 * Two things, both of which cost a frame and neither of which is visible in a diff. A
 * `restartable` composable that is not also `skippable` re-executes whenever its parent
 * recomposes, unconditionally — in a list row, once per row per frame. And a composable taking a
 * parameter the compiler cannot prove immutable stays skippable under strong skipping but pays an
 * `equals` on that parameter every recomposition instead, which for a `List` is O(n) per frame and
 * for a `var`-holding class is a comparison that never says equal.
 *
 * The cause of the second is almost always a `List<T>` where a `PersistentList<T>` would do, a
 * type from a module without the compiler plugin, or a `var` in a class being passed as state.
 * When the type belongs to a library this project cannot change, `config/compose-stability.conf`
 * is where it is declared immutable — that file is why `IntRange` is not on this list.
 *
 * ## Scoped to the design system
 *
 * `:core:designsystem` and `:core:ui` only. Those are the leaves every screen calls, so a
 * component that cannot skip is paid for by every feature that uses it. A feature's own screen is
 * called once per navigation and the same defect there is worth a fraction as much — checking it
 * everywhere would cost every module the ~15% compile overhead metrics carry, to catch things
 * nobody would act on.
 *
 * ## Why there is a baseline
 *
 * The same reason detekt has one and roborazzi records before it verifies: a check retrofitted
 * onto existing code either starts green with the current state written down, or starts red and
 * is switched off within a week. [RecordComposeStabilityTask] writes the file;
 * [CheckComposeStabilityTask] fails on anything not in it. Deleting a line is how a fix is
 * recorded.
 */
internal fun Project.configureComposeStability() {
    if (path !in STABILITY_CHECKED_MODULES) return

    // Set here rather than beside the `enableComposeCompilerMetrics` property in
    // [configureCompose], so that the whole feature — which modules, why, and the tasks that read
    // the result — is one file. These two always write a report; every other module still only
    // writes one when the property asks for it.
    extensions.configure<ComposeCompilerGradlePluginExtension> {
        // `reportsDestination`, not `metricsDestination`. The latter writes a module-level JSON
        // of totals — 295 composables, 153 skippable — which says a number is bad without saying
        // which function it is. The per-composable text file that names them is the report.
        reportsDestination.set(layout.buildDirectory.dir("compose_reports"))
    }

    // A file tree rather than the directory itself: the compiler has not written it at
    // configuration time, and Gradle rejects an @InputDirectory that does not exist.
    val reportsDir = layout.buildDirectory.dir("compose_reports")

    /*
     * Declared as an output of the compilation that writes it.
     *
     * Without this the report is a file Gradle knows nothing about: a second build finds the
     * compile UP-TO-DATE, or restores it FROM-CACHE on CI, and the directory is simply not there.
     * The check would then read nothing and pass — the worst failure mode a check has, because it
     * is indistinguishable from passing honestly. Declaring it makes it cached and restored with
     * everything else the compile produces.
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
 * The compilation that writes the report.
 *
 * One variant, not all of them: these are library modules with no flavours, and the two build
 * types compile the same sources to the same stability answers.
 */
private const val COMPILE_TASK = "compileDebugKotlin"

/**
 * Every composable the report says is paying for a recomposition it did not need, as `FunctionName`.
 *
 * Two distinct findings, one list, because the fix for both starts by opening the same file:
 *
 * * **restartable but not skippable.** The function re-executes whenever its parent recomposes,
 *   unconditionally. In a list row that is once per row per frame.
 * * **an unstable parameter.** Strong skipping keeps the function skippable, but by comparing that
 *   parameter with `equals` on every recomposition instead of skipping the comparison — which for
 *   a `List` is O(n) per frame, and for a `var`-holding class is a comparison that never says
 *   equal.
 *
 * The name alone rather than the whole report line: the line carries the parameter list and the
 * scheme string, and both change for reasons unrelated to stability. A renamed parameter would
 * otherwise read as a new violation and a fixed one as still present.
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
