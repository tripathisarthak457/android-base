package com.example.cmp.buildlogic.guard

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.register

/**
 * Registers `verifyCodeRules`, which fails on the patterns that make a Compose app janky, heavy,
 * untranslatable, phone-only or hard to read (the list is [CodeRules.ALL]), and wires it into
 * `check`.
 *
 * Rules are scoped by where a file sits in the project (`feature/…`, `core/ui/…`), not by Gradle
 * path, so a modular project's `feature/orders/impl` is held to the same rules as a compact
 * project's `feature/orders`.
 */
internal fun Project.verifyCodeRules() {
    val projectDirectory = layout.projectDirectory
    val relative = rootDir.toPath().relativize(projectDir.toPath()).toString().replace('\\', '/')
    val accepted = isolated.rootProject.projectDirectory.file(CodeRulesBaseline.FILE)

    val task = tasks.register<VerifyCodeRulesTask>("verifyCodeRules") {
        group = "verification"
        description = "Fails on forbidden patterns: ripple, composed, raw tweens, Dispatchers.IO and the rest."
        // The build file too: a compiler option there can do what an annotation does in a source file.
        sources.from(fileTree("src") { include("**/*.kt") }, projectDirectory.file("build.gradle.kts"))
        moduleDirectory.set(projectDirectory)
        modulePath.set(relative)
        // Resolved lazily: the convention plugins apply the Compose compiler after this runs.
        composeCompilerApplied.set(provider { pluginManager.hasPlugin("org.jetbrains.kotlin.plugin.compose") })
        if (accepted.asFile.exists()) baseline.set(accepted)
        marker.set(layout.buildDirectory.file("guards/verifyCodeRules.ok"))
    }
    // The design system's rules also run before it builds, so a component that breaks one fails an
    // Android Studio run, not only CI.
    val hooked = if (path == DESIGN_SYSTEM) setOf("check", "preBuild") else setOf("check")
    tasks.named { it in hooked }.configureEach { dependsOn(task) }
}

private const val DESIGN_SYSTEM = ":core:designsystem"

@CacheableTask
internal abstract class VerifyCodeRulesTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    /** The module's directory relative to the project root, which is what the rules are scoped by. */
    @get:Input
    abstract val modulePath: Property<String>

    @get:Input
    abstract val composeCompilerApplied: Property<Boolean>

    /** Findings accepted until their owner fixes them; absent when nothing is. */
    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val baseline: RegularFileProperty

    /** Only turns absolute paths into relative ones. Not an input, so the cache key stays machine-independent. */
    @get:Internal
    abstract val moduleDirectory: DirectoryProperty

    @get:OutputFile
    abstract val marker: RegularFileProperty

    @TaskAction
    fun verify() {
        val base = moduleDirectory.get().asFile
        val module = modulePath.get()
        val findings = sources.files
            .filter { it.isFile }
            .sortedBy { it.path }
            .associate { file ->
                val path = listOf(module, file.relativeTo(base).invariantSeparatorsPath)
                    .filter { it.isNotEmpty() }
                    .joinToString("/")
                path to CodeRules.check(path, file.readText(), composeCompilerApplied.get())
            }
        val accepted = baseline.orNull?.asFile?.readText()?.let(CodeRulesBaseline::parse) ?: CodeRulesBaseline.EMPTY
        val reports = accepted.judge(findings)

        if (reports.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("Code rules broken:")
                    appendLine()
                    reports.forEach { appendLine("  $it") }
                    appendLine()
                    append(
                        "Each rule keeps the app smooth, light, adaptive, translatable or readable; AGENTS.md " +
                            "and docs/PERFORMANCE.md say why. A justified exception goes on the line above: " +
                            "// allow(<rule-id>): <reason>",
                    )
                },
            )
        }

        marker.get().asFile.writeText("ok")
    }
}

/** The rules, apart from Gradle so each one can be unit tested on a source string. */
internal object CodeRules {

    data class Finding(val line: Int, val rule: String, val message: String)

    /**
     * Where a file sits: `feature/orders/impl/src/main/kotlin/…` → module `feature/orders/impl`, set `main`.
     * A module's build file, `feature/orders/build.gradle.kts`, has the module and no source set.
     */
    class Location(val path: String) {
        val isBuildFile: Boolean = path.endsWith(".gradle.kts")
        val module: String = when {
            isBuildFile -> path.substringBeforeLast('/', missingDelimiterValue = "")
            else -> path.substringBefore("/src/", missingDelimiterValue = "")
        }
        val sourceSet: String = path.substringAfter("/src/", missingDelimiterValue = "").substringBefore('/')
        val fileName: String = path.substringAfterLast('/')

        /** `test`, `androidTest`, `testFixtures`, `testDebug`, `commonTest` and the rest. */
        val isTest: Boolean = sourceSet.contains("test", ignoreCase = true)

        /** Shipped code: `main` and the variant source sets beside it (`dev`, `debug`, …). */
        val isProduction: Boolean = !isTest

        val inFeature: Boolean = inTier("feature")
        val inData: Boolean = inTier("data")
        val inCoreUi: Boolean = module == "core/ui"
        val inDesignSystem: Boolean = module == "core/designsystem"
        /** The navigation runtime and the shell nested in it. */
        val inNavigation: Boolean = inTier("core/navigation")
        val inApp: Boolean = module == "app"

        /** The part of the path inside the module's source set, `kotlin/com/acme/…/File.kt`. */
        val inSourceSet: String = path.substringAfter("/src/$sourceSet/", missingDelimiterValue = path)

        fun inTier(tier: String): Boolean = module == tier || module.startsWith("$tier/")
    }

    class Rule(
        val id: String,
        val advice: String,
        val applies: (Location) -> Boolean,
        val lines: (KotlinSource, Location) -> List<Int>,
        /** The page under `.agents/skills/` that explains the rule and its fix, for whoever has to make it. */
        val reference: String? = null,
        /** Whether a module's build file can break it too; every other rule reads Kotlin sources only. */
        val buildFiles: Boolean = false,
    ) {
        val message: String = if (reference == null) advice else "$advice (read .agents/skills/$reference)"
    }

    // The same rules with and without UI components, but for the sheet's: they guard how AppSheet
    // is used and built, and come with it.
    val ALL: List<Rule> =
        ComposeRules.ALL + KotlinRules.ALL + StructureRules.ALL + AdaptiveRules.ALL +
            SheetRules.ALL +
            ScrollRules.ALL + ComponentRules.ALL

    /** Not a source pattern: a `@Composable` built without the Compose compiler crashes when first called. */
    private const val COMPOSE_COMPILER = "compose-compiler"
    private const val COMPOSE_COMPILER_ADVICE =
        "@Composable needs the Compose compiler; apply com.example.cmp.android.compose (or .feature)"

    private val RULE_IDS = ALL.map { it.id }.toSet() + COMPOSE_COMPILER
    private val ALLOW = Regex("""^//\s*allow\(([^)]*)\)\s*(?::\s*(.*))?$""")

    fun check(path: String, text: String, composeCompilerApplied: Boolean = true): List<Finding> {
        val location = Location(path)
        val source = KotlinSource(text)

        val found = ALL.filter { (it.buildFiles || !location.isBuildFile) && it.applies(location) }
            .flatMap { rule -> rule.lines(source, location).map { Finding(it, rule.id, rule.message) } }
            .toMutableList()
        if (!composeCompilerApplied && !location.isBuildFile) {
            source.codeLines.indexOfFirst { "@Composable" in it }.takeIf { it >= 0 }?.let {
                found += Finding(it + 1, COMPOSE_COMPILER, COMPOSE_COMPILER_ADVICE)
            }
        }

        val markers = allowMarkers(source)
        // ktlint keeps comments out of an import list, so a marker on the line above the first import
        // speaks for every import in the list.
        val imports = source.codeLines.indices.filter { source.codeLines[it].trimStart().startsWith("import ") }
            .map { it + 1 }
            .toSet()
        val listMarker = imports.minOrNull()?.let { it - 1 }
        val used = mutableSetOf<Pair<Int, String>>()
        val kept = found.filter { finding ->
            val above = finding.line - 1
            val line = if (finding.line in imports && above !in markers && listMarker != null) listMarker else above
            val marker = markers[line]
            val silenced = marker != null && finding.rule in marker.rules && marker.reason.isNotBlank()
            if (silenced) used += line to finding.rule
            !silenced
        }

        val problems = markers.flatMap { (line, marker) ->
            buildList {
                if (marker.reason.isBlank()) {
                    add(Finding(line, "allow", "an allow marker needs a reason: // allow(${marker.rules.joinToString()}): <why>"))
                }
                marker.rules.filter { it !in RULE_IDS }.forEach {
                    add(Finding(line, "allow", "no rule is called '$it'; rules: ${RULE_IDS.sorted().joinToString()}"))
                }
                marker.rules.filter { it in RULE_IDS && (line to it) !in used && marker.reason.isNotBlank() }.forEach {
                    add(Finding(line, "allow", "nothing on the next line breaks '$it'; delete the marker"))
                }
            }
        }

        return (kept + problems).sortedWith(compareBy({ it.line }, { it.rule }))
    }

    /** A rule that matches [regex] on code lines: imports only, or everything but imports. */
    fun pattern(regex: Regex, imports: Boolean = false): (KotlinSource, Location) -> List<Int> = { source, _ ->
        source.codeLines.mapIndexedNotNull { index, line ->
            if (line.trimStart().startsWith("import ") != imports) return@mapIndexedNotNull null
            if (regex.containsMatchIn(line)) index + 1 else null
        }
    }

    /** A rule that matches [regex] anywhere in the code, imports included. */
    fun anywhere(regex: Regex): (KotlinSource, Location) -> List<Int> = { source, _ ->
        source.codeLines.mapIndexedNotNull { index, line -> if (regex.containsMatchIn(line)) index + 1 else null }
    }

    /** The line of the `package` declaration, where findings about a whole file are reported. */
    fun packageLine(source: KotlinSource): Int =
        source.codeLines.indexOfFirst { it.trimStart().startsWith("package ") }.let { if (it < 0) 1 else it + 1 }

    private class Marker(val rules: List<String>, val reason: String)

    /** Allow markers by line, each alone on its line. */
    private fun allowMarkers(source: KotlinSource): Map<Int, Marker> =
        source.lineComments.mapNotNull { (line, comment) ->
            if (!comment.alone) return@mapNotNull null
            val match = ALLOW.matchEntire(comment.raw.trim()) ?: return@mapNotNull null
            val rules = match.groupValues[1].split(',').map { it.trim() }.filter { it.isNotEmpty() }
            line to Marker(rules, match.groupValues[2].trim())
        }.toMap()
}
