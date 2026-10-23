package io.example.fitclocal.buildlogic.guard

import io.example.fitclocal.buildlogic.guard.CodeRules.Rule
import io.example.fitclocal.buildlogic.guard.CodeRules.packageLine
import io.example.fitclocal.buildlogic.guard.CodeRules.pattern
import io.example.fitclocal.buildlogic.guard.KotlinSource.Comment

/** Rules for Kotlin in every module: threads and coroutines, null safety, comments and file size. */
internal object KotlinRules {

    /** A file longer than this has more than one concept in it (CODE-STANDARDS K-2). */
    const val MAX_FILE_LINES = 500

    val ALL: List<Rule> = listOf(
        Rule(
            id = "dispatchers-io",
            advice = "inject AppDispatchers and use dispatchers.io or dispatchers.default",
            applies = { it.isProduction && !(it.inApp && "/di/" in it.path) },
            lines = pattern(Regex("""\bDispatchers\s*\.\s*(IO|Default)\b""")),
        ),
        Rule(
            id = "blocking",
            advice = "never block a thread or use GlobalScope; launch from a ViewModel or ApplicationScope",
            applies = { it.isProduction },
            lines = pattern(Regex("""\b(runBlocking|GlobalScope)\b""")),
        ),
        Rule(
            id = "run-catching",
            advice = "runCatching in a coroutine swallows its cancellation; use runSuspendCatching or catch what you expect",
            applies = { it.isProduction && (it.inTier("core") || it.inData || it.inApp) },
            lines = { source, _ ->
                RUN_CATCHING.findAll(source.code)
                    .filter { Declarations.suspends(source, it.range.first) }
                    .map { source.lineOf(it.range.first) }
                    .distinct()
                    .toList()
            },
        ),
        Rule(
            id = "bang-bang",
            advice = "restructure so the value cannot be null here, or checkNotNull(x) { \"why it cannot be\" }",
            applies = { it.isProduction },
            lines = pattern(Regex("""!!""")),
        ),
        Rule(
            id = "slop-comment",
            advice = "say why the code is this way; the history of a change belongs in its commit message, and a " +
                "TODO needs an allow marker naming its owner and the condition that ends it",
            applies = { true },
            lines = { source, _ -> source.comments.filter(::isSlop).map { it.line } },
        ),
        Rule(
            id = "commented-code",
            advice = "delete code that is commented out; git keeps it",
            applies = { true },
            lines = { source, _ ->
                source.comments.filter { it.kind == Comment.Kind.LINE && isCode(it.content) }.map { it.line }
            },
        ),
        Rule(
            id = "file-length",
            advice = "split the file by concept; more than $MAX_FILE_LINES lines is more than one thing",
            applies = { it.isProduction },
            lines = { source, _ -> if (source.lineCount > MAX_FILE_LINES) listOf(packageLine(source)) else emptyList() },
        ),
        Rule(
            id = "spaced-test-name",
            advice = "device tests run on runtimes that refuse spaces in method names; use camelCase_with_underscores",
            applies = { it.sourceSet == "androidTest" || it.sourceSet == "commonTest" || it.module.startsWith("benchmark") },
            lines = { source, _ ->
                source.codeAndStringLines.mapIndexedNotNull { index, line ->
                    if (SPACED_NAME.containsMatchIn(line)) index + 1 else null
                }
            },
        ),
    )

    private val RUN_CATCHING = Regex("""(?<![\w.])runCatching\s*\{|\.\s*runCatching\s*\{""")
    private val SPACED_NAME = Regex("""\bfun\s+`[^`\n]*\s[^`\n]*`""")

    // "Removed when the user signs out" describes the code; "Removed the retry" narrates a change.
    private val SLOP = Regex(
        """^(?:(?:Added|Updated|Fixed|Changed|Removed)(?::|\s+(?:a|an|the|to|from|this|that|these|those|support|""" +
            """unused|missing|bugs?|crash|issue|for|per)\b)|New:|Step\s+\d+|As requested\b|Per review\b|""" +
            """IMPORTANT\b|NOTE:|CRITICAL\b|PERF:|HACK\b|FIXME\b|TODO\b|XXX\b)""",
    )
    private val BANNER = Regex("""^[─━═=\-*#\s]{4,}$|── """)
    private val CODE_START = Regex(
        """^(?:(?:val|var|fun|import|class|object|override|private|internal|return)\s|(?:if|for|while)\s*\(|when\s*[({])""",
    )
    private val ALLOW_MARKER = Regex("""^allow\(""")

    private fun isSlop(comment: Comment): Boolean {
        val content = comment.content
        return SLOP.containsMatchIn(content) || content.lineSequence().any { BANNER.containsMatchIn(it.trim()) }
    }

    /** A `//` comment that reads as a line of Kotlin rather than a sentence. */
    private fun isCode(content: String): Boolean {
        if (content.isEmpty() || ALLOW_MARKER.containsMatchIn(content) || content.startsWith("<")) return false
        // Not `;`: Kotlin needs none, and prose that runs on to the next line often ends in one.
        return CODE_START.containsMatchIn(content) || content.last() in "{}"
    }
}
