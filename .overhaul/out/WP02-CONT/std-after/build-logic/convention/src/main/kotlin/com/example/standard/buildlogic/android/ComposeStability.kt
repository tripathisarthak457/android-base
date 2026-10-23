package com.example.standard.buildlogic.android

import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.register
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/**
 * The modules whose composables every screen calls: a parameter compared by identity here costs a
 * recomposition in every screen that uses them. Their stability report is always written.
 */
internal val STABILITY_CHECKED_MODULES = setOf(":core:designsystem", ":core:ui", ":core:navigation")

/** Registers `checkComposeStability`, which reads the compiler's report, and wires it into `check`. */
internal fun Project.configureComposeStability() {
    if (path !in STABILITY_CHECKED_MODULES) return

    // Every Kotlin compilation of the module writes its report to the same file here, since a test
    // compilation takes the module's name to see its `internal` declarations: the file holds
    // whichever compilation ran last. The main compilation's is copied aside as it ends, and that
    // copy is all the check reads.
    val reportsDir = layout.buildDirectory.dir(COMPOSE_REPORTS)
    val stabilityReport = layout.buildDirectory.dir(STABILITY_REPORT)

    tasks.matching { it.name == COMPILE_TASK }.configureEach {
        outputs.dir(stabilityReport).withPropertyName("composeStabilityReport")
        doLast("copyComposeStabilityReport") {
            copyReports(from = reportsDir.get().asFile, to = stabilityReport.get().asFile)
        }
    }

    // A file tree rather than the directory itself: the compiler has not written it at
    // configuration time, and Gradle rejects an @InputDirectory that does not exist.
    val written = fileTree(stabilityReport) {
        include("**/*-composables.txt")
    }
    val baseline = rootProject.layout.projectDirectory.file(ComposeStability.BASELINE)

    val check = tasks.register<CheckComposeStabilityTask>("checkComposeStability") {
        group = "verification"
        description = "Fails on a composable parameter the compiler compares by identity."
        reports.setFrom(written)
        if (baseline.asFile.exists()) baselineFile.set(baseline)
        modulePath.set(path)
        marker.set(layout.buildDirectory.file("guards/checkComposeStability.ok"))
        // The report is a by-product of compiling, so the task that writes it has to have run.
        dependsOn(COMPILE_TASK)
    }

    val record = tasks.register<RecordComposeStabilityTask>("recordComposeStability") {
        group = "verification"
        description = "Adds the current identity-compared parameters to the baseline, without reasons."
        reports.setFrom(written)
        baselineFile.set(baseline)
        dependsOn(COMPILE_TASK)
    }

    // The compiler reports only the files it compiles, so an incremental compile leaves a report of
    // the files that changed and nothing else: read alone, it would pass every composable it left
    // out. A build that reads the report compiles the module in full, and the compile carries that
    // as an input, so a report an earlier incremental compile wrote is never taken for a whole one.
    gradle.taskGraph.whenReady {
        val reads = hasTask(check.get()) || hasTask(record.get())
        tasks.named(COMPILE_TASK).configure {
            inputs.property("composeStabilityReportIsWhole", reads)
            if (reads) (this as KotlinCompile).incremental = false
        }
    }

    // There is nothing to read until the module has been compiled, so it runs with `check`.
    tasks.matching { it.name == "check" }.configureEach { dependsOn(check) }
}

/** Replaces what [to] holds with the reports in [from]. */
internal fun copyReports(from: File, to: File) {
    to.deleteRecursively()
    to.mkdirs()
    from.listFiles { file -> file.name.endsWith("-composables.txt") }.orEmpty().forEach { report ->
        report.copyTo(to.resolve(report.name))
    }
}

/**
 * The compilation that writes the report. One variant, not all of them: these are library modules
 * with no flavours, and the two build types compile the same sources to the same stability answers.
 */
private const val COMPILE_TASK = "compileDebugKotlin"

/** Where the check finds the report of [COMPILE_TASK], under the module's build directory. */
private const val STABILITY_REPORT = "compose_stability"

/** The report reading and the baseline, apart from Gradle so they can be unit tested. */
internal object ComposeStability {

    const val BASELINE = "config/compose-stability-baseline.txt"

    /** `restartable skippable scheme(…) fun <T> com.acme.AppPicker(` → `com.acme.AppPicker`. */
    private val FUNCTION = Regex("""\bfun\s+(?:<[^>]*>\s+)?([\w.]+)\s*\(""")

    /** `  unstable items: List<T>` → `items`. */
    private val UNSTABLE = Regex("""^\s+unstable\s+([\w<>]+):""")

    /**
     * Every problem in the reports, as `fqName(parameter)` for a parameter of a type the compiler
     * cannot prove stable, and `fqName: not skippable` for a restartable function that cannot skip,
     * which under strong skipping only happens when the compiler flags were changed.
     */
    fun problems(reports: List<String>): List<String> =
        reports.flatMap { report ->
            var function: String? = null
            report.lines().mapNotNull { line ->
                FUNCTION.find(line)?.let { declaration ->
                    function = declaration.groupValues[1]
                    val unskippable = line.startsWith("restartable ") && " skippable " !in line
                    return@mapNotNull if (unskippable) "${declaration.groupValues[1]}: not skippable" else null
                }
                UNSTABLE.find(line)?.let { parameter -> function?.let { "$it(${parameter.groupValues[1]})" } }
            }
        }.distinct().sorted()

    class Baseline(val accepted: Set<String>, val unexplained: List<String>)

    /** Entries are `fqName(parameter)  # reason`; an entry without a reason is listed as unexplained. */
    fun baseline(text: String): Baseline {
        val entries = text.lines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }
        return Baseline(
            accepted = entries.map { it.substringBefore('#').trim() }.toSet(),
            unexplained = entries.filter { it.substringAfter('#', missingDelimiterValue = "").isBlank() },
        )
    }
}

@CacheableTask
internal abstract class CheckComposeStabilityTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val reports: ConfigurableFileCollection

    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val baselineFile: RegularFileProperty

    @get:Input
    abstract val modulePath: Property<String>

    @get:OutputFile
    abstract val marker: RegularFileProperty

    @TaskAction
    fun verify() {
        val baseline = ComposeStability.baseline(baselineFile.asFile.orNull?.readText().orEmpty())
        if (baseline.unexplained.isNotEmpty()) {
            throw GradleException(
                "${ComposeStability.BASELINE} accepts entries without a reason. Say why each one has to " +
                    "stay, after a `#` on its line:\n\n" + baseline.unexplained.joinToString("\n") { "  $it" },
            )
        }

        val present = reports.files.filter { it.isFile }
        if (present.isEmpty()) {
            throw GradleException(
                "${modulePath.get()}: the Compose compiler wrote no stability report, so this check has nothing " +
                    "to read. That is a build configuration problem rather than a clean result: silence here " +
                    "would be indistinguishable from passing. Check that reportsDestination is still set in Compose.kt.",
            )
        }

        val fresh = ComposeStability.problems(present.sortedBy { it.path }.map(File::readText)) - baseline.accepted
        if (fresh.isEmpty()) {
            marker.get().asFile.writeText("ok")
            return
        }

        throw GradleException(
            """
            ${modulePath.get()} has composable parameters the Compose compiler compares by identity:

            ${fresh.joinToString("\n            ") { "  $it" }}

            Under strong skipping such a parameter is compared with ===, so the composable reruns
            whenever its caller passes a new instance, even an equal one. Pass a stable type (a
            read-only collection, an @Immutable class, a type listed in config/compose-stability.conf),
            make it a () -> T provider, or accept it in ${ComposeStability.BASELINE} with a
            `# reason` in a commit that says why. Read build/$STABILITY_REPORT/*-composables.txt for
            the parameter's type.
            """.trimIndent(),
        )
    }
}

@DisableCachingByDefault(because = "Appends to the checked-in baseline when someone asks it to.")
internal abstract class RecordComposeStabilityTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val reports: ConfigurableFileCollection

    @get:OutputFile
    abstract val baselineFile: RegularFileProperty

    @TaskAction
    fun record() {
        val file = baselineFile.get().asFile
        val existing = file.takeIf { it.isFile }?.readLines().orEmpty()
        val accepted = ComposeStability.baseline(existing.joinToString("\n")).accepted

        // Appended rather than rewritten: each module writes its own report, and recording from one
        // module must keep every other module's entries and their reasons.
        val added = ComposeStability.problems(reports.files.filter { it.isFile }.map(File::readText)) - accepted
        file.parentFile.mkdirs()
        file.writeText((existing + added).joinToString("\n", postfix = "\n"))
        logger.lifecycle("Added ${added.size} entries to ${file.path}; the check fails until each has a `# reason`.")
    }
}
