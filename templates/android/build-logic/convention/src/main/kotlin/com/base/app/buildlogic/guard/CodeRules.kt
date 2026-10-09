package com.base.app.buildlogic.guard

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
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
 * Registers `verifyCodeRules`, which fails on the patterns that make a Compose app janky, heavy
 * or untranslatable (the list is [CodeRules.ALL]), and wires it into `check`.
 *
 * Rules are scoped by where a file sits in the project (`feature/…`, `core/ui/…`), not by Gradle
 * path, so a modular project's `feature/orders/impl` is held to the same rules as a compact
 * project's `feature/orders`.
 */
internal fun Project.verifyCodeRules() {
    val projectDirectory = layout.projectDirectory
    val relative = rootDir.toPath().relativize(projectDir.toPath()).toString().replace('\\', '/')

    val task = tasks.register<VerifyCodeRulesTask>("verifyCodeRules") {
        group = "verification"
        description = "Fails on forbidden patterns: ripple, composed, raw tweens, Dispatchers.IO and the rest."
        sources.from(fileTree("src") { include("**/*.kt") })
        moduleDirectory.set(projectDirectory)
        modulePath.set(relative)
        // Resolved lazily: the convention plugins apply the Compose compiler after this runs.
        composeCompilerApplied.set(provider { pluginManager.hasPlugin("org.jetbrains.kotlin.plugin.compose") })
        marker.set(layout.buildDirectory.file("guards/verifyCodeRules.ok"))
    }
    tasks.named { it == "check" }.configureEach { dependsOn(task) }
}

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
            .flatMap { file ->
                val path = listOf(module, file.relativeTo(base).invariantSeparatorsPath)
                    .filter { it.isNotEmpty() }
                    .joinToString("/")
                CodeRules.check(path, file.readText(), composeCompilerApplied.get())
                    .map { "$path:${it.line} ${it.rule}: ${it.message}" }
            }

        if (findings.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("Code rules broken:")
                    appendLine()
                    findings.forEach { appendLine("  $it") }
                    appendLine()
                    append(
                        "Each rule keeps the app smooth, light or translatable; see MOTION-AND-PERFORMANCE.md. " +
                            "A justified exception goes on the line above: // allow(<rule-id>): <reason>",
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

    /** Where a file sits: `feature/orders/impl/src/main/kotlin/…` → module `feature/orders/impl`, set `main`. */
    class Location(val path: String) {
        val module: String = path.substringBefore("/src/", missingDelimiterValue = "")
        private val sourceSet: String = path.substringAfter("/src/", missingDelimiterValue = "").substringBefore('/')

        /** `test`, `androidTest`, `testFixtures`, `testDebug` and the rest. */
        val isTest: Boolean = sourceSet.contains("test", ignoreCase = true)
        val isMain: Boolean = sourceSet == "main"

        val inFeature: Boolean = module == "feature" || module.startsWith("feature/")
        val inCoreUi: Boolean = module == "core/ui"
    }

    class Rule(
        val id: String,
        val advice: String,
        val applies: (Location) -> Boolean,
        val lines: (KotlinSource) -> List<Int>,
    )

    private fun pattern(regex: Regex, imports: Boolean = false): (KotlinSource) -> List<Int> = { source ->
        source.codeLines.mapIndexedNotNull { index, line ->
            if (line.trimStart().startsWith("import ") != imports) return@mapIndexedNotNull null
            if (regex.containsMatchIn(line)) index + 1 else null
        }
    }

    private val uiCode: (Location) -> Boolean = { (it.inFeature || it.inCoreUi) && !it.isTest }

    val ALL: List<Rule> = listOf(
        Rule(
            id = "ripple",
            advice = "no ripple or grey overlay; use Modifier.pressable(onClick, feedback = PressFeedback.…)",
            applies = { true },
            lines = pattern(Regex("""^\s*import\s+androidx\.compose\.material\w*\.ripple\b"""), imports = true),
        ),
        Rule(
            id = "material",
            advice = "Compose Material is not a dependency; use the design system's component",
            applies = { true },
            lines = pattern(Regex("""^\s*import\s+androidx\.compose\.material(?!\w*\.ripple\b)"""), imports = true),
        ),
        Rule(
            id = "composed",
            advice = "Modifier.composed allocates per call site; write a Modifier.Node (ModifierNodeElement)",
            applies = { true },
            lines = pattern(Regex("""(?<![\w.])composed\s*[({]|\.\s*composed\s*[({]""")),
        ),
        Rule(
            id = "nav-fade",
            advice = "screens do not set NavDisplay transitions; pick a NavTransitionStyle from the registry",
            applies = { it.inFeature && !it.isTest },
            lines = pattern(Regex("""\bNavDisplay\s*\.\s*(transitionSpec|popTransitionSpec|predictivePopTransitionSpec)\b""")),
        ),
        Rule(
            id = "collect-as-state",
            advice = "collectAsState keeps collecting in the background; use collectAsStateWithLifecycle()",
            applies = uiCode,
            lines = pattern(Regex("""\bcollectAsState\s*\(""")),
        ),
        Rule(
            id = "dispatchers-io",
            advice = "inject AppDispatchers and use dispatchers.io or dispatchers.default",
            applies = { !it.isTest && !(it.module == "app" && "/di/" in it.path) },
            lines = pattern(Regex("""\bDispatchers\s*\.\s*(IO|Default)\b""")),
        ),
        Rule(
            id = "raw-tween",
            advice = "use a spring from AppTheme.springs, which follows the motion style and reduce motion",
            applies = uiCode,
            lines = pattern(Regex("""(?<![\w.])(tween|spring)\s*(<[^>]*>)?\s*\(""")),
        ),
        Rule(
            id = "raw-color",
            advice = "use a colour role from AppTheme.colors, which follows dark mode and the brand",
            applies = uiCode,
            lines = pattern(Regex("""\bColor\s*\(\s*0[xX]""")),
        ),
        Rule(
            id = "raw-dp",
            advice = "use AppTheme.spacing or a size token; only 0.dp and 1.dp are written raw",
            applies = { it.inFeature && !it.isTest },
            lines = { source -> rawDpLines(source) },
        ),
        Rule(
            id = "subcompose-image",
            advice = "SubcomposeAsyncImage composes per load state and stutters in lists; use AsyncImage",
            applies = uiCode,
            lines = pattern(Regex("""\bSubcomposeAsyncImage\b""")),
        ),
        Rule(
            id = "bare-clickable",
            advice = "say which press feedback a control uses: Modifier.pressable(onClick, feedback = PressFeedback.…)",
            applies = uiCode,
            lines = pattern(Regex("""\.\s*(clickable|combinedClickable)\s*[({]""")),
        ),
        Rule(
            id = "lazy-key",
            advice = "give lazy rows a stable key = { … } so they keep their state and the scroll position",
            applies = uiCode,
            lines = { source -> LazyKeys.unkeyed(source) },
        ),
        Rule(
            id = "blocking",
            advice = "never block a thread or use GlobalScope; launch from a ViewModel or ApplicationScope",
            applies = { !it.isTest },
            lines = pattern(Regex("""\b(runBlocking|GlobalScope)\b""")),
        ),
        Rule(
            id = "hardcoded-copy",
            advice = "move the copy to src/main/res/values/strings.xml and read it with stringResource or UiText",
            applies = { location ->
                location.isMain && (location.inFeature || location.module in LOCALISED_CORE)
            },
            lines = { source -> HardcodedCopy.lines(source) },
        ),
    )

    /** Not a source pattern: a `@Composable` built without the Compose compiler crashes when first called. */
    private const val COMPOSE_COMPILER = "compose-compiler"
    private const val COMPOSE_COMPILER_ADVICE =
        "@Composable needs the Compose compiler; apply com.base.app.android.compose (or .feature)"

    /** Core modules that draw copy a user reads, and so are held to the same rule as features. */
    private val LOCALISED_CORE = setOf("app", "core/designsystem", "core/ui")

    private val RULE_IDS = ALL.map { it.id }.toSet() + COMPOSE_COMPILER
    private val ALLOW = Regex("""^//\s*allow\(([^)]*)\)\s*(?::\s*(.*))?$""")
    private val RAW_DP = Regex("""(?<![\w.])(\d+(?:\.\d+)?)[fF]?\s*\.\s*dp\b""")

    fun check(path: String, text: String, composeCompilerApplied: Boolean = true): List<Finding> {
        val location = Location(path)
        val source = KotlinSource(text)

        val found = ALL.filter { it.applies(location) }
            .flatMap { rule -> rule.lines(source).map { Finding(it, rule.id, rule.advice) } }
            .toMutableList()
        if (!composeCompilerApplied) {
            source.codeLines.indexOfFirst { "@Composable" in it }.takeIf { it >= 0 }?.let {
                found += Finding(it + 1, COMPOSE_COMPILER, COMPOSE_COMPILER_ADVICE)
            }
        }

        val markers = allowMarkers(source)
        val used = mutableSetOf<Pair<Int, String>>()
        val kept = found.filter { finding ->
            val marker = markers[finding.line - 1]
            val silenced = marker != null && finding.rule in marker.rules && marker.reason.isNotBlank()
            if (silenced) used += (finding.line - 1) to finding.rule
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

    private class Marker(val rules: List<String>, val reason: String)

    /** Allow markers by line, each alone on its line. */
    private fun allowMarkers(source: KotlinSource): Map<Int, Marker> =
        source.lineComments.mapNotNull { (line, comment) ->
            if (!comment.alone) return@mapNotNull null
            val match = ALLOW.matchEntire(comment.text.trim()) ?: return@mapNotNull null
            val rules = match.groupValues[1].split(',').map { it.trim() }.filter { it.isNotEmpty() }
            line to Marker(rules, match.groupValues[2].trim())
        }.toMap()

    private fun rawDpLines(source: KotlinSource): List<Int> =
        source.codeLines.mapIndexedNotNull { index, line ->
            val raw = RAW_DP.findAll(line).any { it.groupValues[1].toDouble() !in setOf(0.0, 1.0) }
            if (raw) index + 1 else null
        }
}

/**
 * `items(…)` and `itemsIndexed(…)` calls in a lazy list, grid or staggered grid scope that pass no
 * `key`. A call is in such a scope when one of its enclosing blocks is the content lambda of a
 * `Lazy…` call or the body of a `Lazy…Scope` extension.
 */
internal object LazyKeys {

    private val CALL = Regex("""(?<![\w.])(items|itemsIndexed)\s*\(""")
    private val LAZY_CALL = Regex("""^Lazy[A-Z]\w*$""")
    private val LAZY_RECEIVER = Regex("""Lazy\w*Scope\s*\.\s*$""")
    private val KEY = Regex("""\bkey\s*=""")
    private val FUN = Regex("""\bfun\s*$""")

    fun unkeyed(source: KotlinSource): List<Int> {
        val code = source.code
        val calls = CALL.findAll(code).filterNot { FUN.containsMatchIn(code.substring(maxOf(0, it.range.first - 8), it.range.first)) }
            .map { it.range.first to it.range.last }
            .toList()
        if (calls.isEmpty()) return emptyList()

        val lazyAt = BooleanArray(code.length)
        val blocks = ArrayDeque<Boolean>()
        for (i in code.indices) {
            when (code[i]) {
                '{' -> blocks.addLast(isLazyBlock(code, i))
                '}' -> blocks.removeLastOrNull()
            }
            lazyAt[i] = blocks.any { it }
        }

        return calls.filter { (start, open) ->
            lazyAt[start] && !KEY.containsMatchIn(arguments(code, open))
        }.map { (start, _) -> source.lineOf(start) }.distinct()
    }

    /** The text between the parenthesis at [open] and its match. */
    private fun arguments(code: String, open: Int): String {
        var depth = 0
        for (i in open until code.length) {
            when (code[i]) {
                '(' -> depth++
                ')' -> if (--depth == 0) return code.substring(open + 1, i)
            }
        }
        return code.substring(open + 1)
    }

    private fun isLazyBlock(code: String, brace: Int): Boolean {
        var end = brace - 1
        while (end >= 0 && code[end].isWhitespace()) end--
        if (end >= 0 && code[end] == ')') {
            var depth = 0
            while (end >= 0) {
                when (code[end]) {
                    ')' -> depth++
                    '(' -> if (--depth == 0) break
                }
                end--
            }
            end--
            while (end >= 0 && code[end].isWhitespace()) end--
        }
        var start = end
        while (start >= 0 && (code[start].isLetterOrDigit() || code[start] == '_')) start--
        val callee = code.substring(start + 1, end + 1)
        val before = code.substring(maxOf(0, start - 80), start + 1)
        return LAZY_CALL.matches(callee) || LAZY_RECEIVER.containsMatchIn(before)
    }
}

/**
 * Copy typed straight into a composable where a string resource belongs. Previews are exempt:
 * their sample data is not shipped.
 */
internal object HardcodedCopy {

    fun lines(source: KotlinSource): List<Int> {
        var inPreview = false
        return source.codeAndStringLines.mapIndexedNotNull { index, line ->
            FUNCTION.find(source.codeLines[index])?.let { inPreview = it.groupValues[1].endsWith("Preview") }
            if (inPreview || source.codeLines[index].isBlank()) return@mapIndexedNotNull null
            if (copyIn(line).any(::isProse)) index + 1 else null
        }
    }

    /** The literals on [line] that end up on screen: copy parameters, and fallbacks turned into UiText. */
    private fun copyIn(line: String): List<String> = buildList {
        COPY_PARAMETER.findAll(line).forEach { add(it.groupValues[2]) }
        if (COPY_PARAMETER.containsMatchIn(line)) ELSE_BRANCH.findAll(line).forEach { add(it.groupValues[1]) }
        if ("UiText" in line) UI_TEXT_FALLBACK.findAll(line).forEach { add(it.groupValues[1]) }
    }

    /** A word or a sentence, once string templates are taken out. "Next" is copy; "${a} ${b}" is not. */
    private fun isProse(literal: String): Boolean {
        val words = literal.replace(TEMPLATE, "").trim()
        return words.matches(CAPITALISED_WORD) || (' ' in words && words.any(Char::isLowerCase))
    }

    private val FUNCTION = Regex("""\bfun\s+(?:[\w.<>, ]+\.)?(\w+)\s*\(""")

    // Any parameter named for copy, including prefixed ones like confirmLabel, and the first
    // branch of an `if` written straight into one.
    private val COPY_PARAMETER = Regex(
        "\\b(\\w*(?:[Tt]ext|[Tt]itle|[Ll]abel|[Hh]elper|[Ss]upporting|[Mm]essage|[Dd]escription|" +
            "[Pp]laceholder|[Oo]verline|[Ss]ubtitle|[Cc]aption))\\s*=\\s*(?:if\\s*\\(.*?\\)\\s*)?\"([^\"]*)\"",
    )
    private val ELSE_BRANCH = Regex("\\belse\\s+\"([^\"]*)\"")
    private val UI_TEXT_FALLBACK = Regex("(?:\\?:\\s*|UiText\\.Dynamic\\(\\s*)\"([^\"]*)\"")
    private val TEMPLATE = Regex("\\$\\{[^}]*}|\\$\\w+")
    private val CAPITALISED_WORD = Regex("[A-Z][a-z].*")
}
