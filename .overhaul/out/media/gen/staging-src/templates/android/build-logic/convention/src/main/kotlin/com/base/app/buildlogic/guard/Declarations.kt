package com.base.app.buildlogic.guard

/**
 * The few declaration shapes the code rules need to see, read from [KotlinSource.code]: composable
 * functions with their parameters, top-level declarations, and whether a point in the file runs
 * inside a coroutine. Like the tokenizer, this is pattern matching over well-formed code, not a
 * parser.
 */
internal object Declarations {

    class Parameter(val name: String, val type: String, val offset: Int)

    class Function(val name: String, val offset: Int, val parameters: List<Parameter>)

    class TopLevel(val line: Int, val keyword: String, val modifiers: Set<String>, val name: String)

    /** Every `@Composable fun`, with its parameter list split at the top-level commas. */
    fun composables(source: KotlinSource): List<Function> {
        val code = source.code
        return COMPOSABLE_FUN.findAll(code).map { match ->
            val open = match.range.last
            val close = closing(code, open)
            Function(
                name = match.groupValues[1],
                offset = checkNotNull(match.groups[1]).range.first,
                parameters = parameters(code, open + 1, close),
            )
        }.toList()
    }

    /** Declarations at the top of the file, outside any class, function or parameter list. */
    fun topLevel(source: KotlinSource): List<TopLevel> {
        val depths = source.depths
        return source.codeLines.mapIndexedNotNull { index, line ->
            val start = source.offsetOf(index + 1)
            if (depths.braces[start] != 0 || depths.parens[start] != 0) return@mapIndexedNotNull null
            val match = TOP_LEVEL.find(line) ?: return@mapIndexedNotNull null
            val modifiers = match.groupValues[1].split(' ').filter { it.isNotEmpty() }.toSet()
            if (modifiers.any { it !in MODIFIERS }) return@mapIndexedNotNull null
            TopLevel(index + 1, match.groupValues[2], modifiers, match.groupValues[3])
        }
    }

    /**
     * True when [offset] runs in a coroutine: inside a `suspend fun`, or inside a lambda passed to
     * a builder whose block suspends (`launch`, `withContext`, `flow`, …). A plain function body
     * between the two ends the search, since nothing in it can suspend.
     */
    fun suspends(source: KotlinSource, offset: Int): Boolean {
        val code = source.code
        var position = offset
        while (true) {
            val open = enclosingBrace(code, position) ?: return expressionBodySuspends(source, position)
            val head = blockHead(code, open)
            when {
                head.function != null -> return "suspend" in head.function
                head.callee in COROUTINE_BUILDERS -> return true
                head.isTypeBody -> return expressionBodySuspends(source, position)
            }
            position = open
        }
    }

    /** The offset of the bracket that closes the one at [open], or the end of the file. */
    fun closing(code: String, open: Int): Int {
        val opener = code[open]
        val closer = PAIRS.getValue(opener)
        var depth = 0
        for (index in open until code.length) {
            when (code[index]) {
                opener -> depth++
                closer -> if (--depth == 0) return index
            }
        }
        return code.length
    }

    /** The offset of the bracket that opens the one closing at [close]. */
    fun opening(code: String, close: Int): Int {
        val closer = code[close]
        val opener = PAIRS.entries.first { it.value == closer }.key
        var depth = 0
        for (index in close downTo 0) {
            when (code[index]) {
                closer -> depth++
                opener -> if (--depth == 0) return index
            }
        }
        return 0
    }

    private fun parameters(code: String, from: Int, to: Int): List<Parameter> {
        val parameters = mutableListOf<Parameter>()
        var depth = 0
        var start = from
        for (index in from..to) {
            val char = if (index < to) code[index] else ','
            when (char) {
                '(', '<', '[', '{' -> depth++
                ')', ']', '}' -> depth--
                // `->` in a function type is not a closing angle bracket.
                '>' -> if (code.getOrNull(index - 1) != '-') depth--
                ',' -> if (depth == 0) {
                    PARAMETER.find(code.substring(start, index))?.let { match ->
                        parameters += Parameter(
                            name = match.groupValues[1],
                            type = match.groupValues[2].trim(),
                            offset = start + checkNotNull(match.groups[1]).range.first,
                        )
                    }
                    start = index + 1
                }
            }
        }
        return parameters
    }

    private class Head(val callee: String?, val function: Set<String>?, val isTypeBody: Boolean)

    /** What the block opening at [open] belongs to: a call's trailing lambda, a function body, or a class body. */
    private fun blockHead(code: String, open: Int): Head {
        val before = code.substring(maxOf(0, open - LOOK_BACK), open)
        if (TYPE_HEAD.containsMatchIn(before)) return Head(null, null, isTypeBody = true)

        var end = open - 1
        while (end >= 0 && code[end].isWhitespace()) end--
        val callee = if (end >= 0 && code[end] == ')') {
            val parenthesis = opening(code, end)
            val head = code.substring(maxOf(0, parenthesis - LOOK_BACK), parenthesis)
            FUNCTION_HEAD.find(head)?.let { return Head(null, modifiers(it.groupValues[1]), false) }
            IDENTIFIER_BEFORE.find(head)?.groupValues?.get(1)
        } else {
            if (RETURN_TYPE_HEAD.containsMatchIn(before)) {
                FUNCTION_KEYWORD.findAll(before).lastOrNull()?.let { return Head(null, modifiers(it.groupValues[1]), false) }
            }
            IDENTIFIER_BEFORE.find(before)?.groupValues?.get(1)
        }
        // Initialisers, accessors and constructors are bodies that never suspend.
        if (callee in PLAIN_BODIES) return Head(null, emptySet(), false)
        return Head(callee, null, false)
    }

    private fun modifiers(words: String): Set<String> = words.split(' ').filter { it.isNotEmpty() }.toSet()

    private fun enclosingBrace(code: String, offset: Int): Int? {
        var depth = 0
        for (index in offset - 1 downTo 0) {
            when (code[index]) {
                '}' -> depth++
                '{' -> if (depth == 0) return index else depth--
            }
        }
        return null
    }

    /** For a point outside every function body: is the declaration it belongs to a `suspend fun = …`? */
    private fun expressionBodySuspends(source: KotlinSource, offset: Int): Boolean {
        val code = source.code
        val depth = source.depths.braces[offset]
        val declaration = DECLARATION.findAll(code.substring(0, offset))
            .lastOrNull { source.depths.braces[it.range.first] == depth }
            ?: return false
        return declaration.groupValues[2] == "fun" && "suspend" in declaration.groupValues[1].split(' ')
    }

    private val PAIRS = mapOf('(' to ')', '{' to '}', '[' to ']')

    private const val ANNOTATION = """@[\w.]+(?:\([^()]*\))?\s+"""
    private const val MODIFIER_WORDS =
        "public|internal|private|protected|inline|override|open|abstract|suspend|actual|expect|operator|infix|tailrec"

    private val COMPOSABLE_FUN = Regex(
        """@Composable\s+(?:$ANNOTATION|(?:$MODIFIER_WORDS)\s+)*fun\s+(?:<[^>]*>\s+)?(?:[\w.<>?, ]+\.)?(\w+)\s*\(""",
    )
    private val PARAMETER = Regex("""^\s*(?:$ANNOTATION)*(?:(?:vararg|noinline|crossinline|val|var)\s+)*(\w+)\s*:\s*([^=]+)""")

    private val MODIFIERS = setOf(
        "public", "internal", "private", "protected", "abstract", "open", "sealed", "data", "enum", "annotation",
        "inline", "value", "suspend", "operator", "infix", "tailrec", "external", "const", "lateinit", "inner",
        "override", "expect", "actual", "fun",
    )
    private val TOP_LEVEL = Regex(
        """^\s*(?:$ANNOTATION)*((?:[a-z]+\s+)*)(fun|class|object|interface|val|var|typealias)\s+(?:<[^>]*>\s+)?(?:[\w.<>?, ]+\.)?`?(\w+)""",
    )

    private const val LOOK_BACK = 300
    private val FUNCTION_HEAD = Regex("""((?:\b(?:$MODIFIER_WORDS)\s+)*)\bfun\s+(?:<[^>]*>\s+)?(?:[\w.<>?, ]+\.)?\w+\s*$""")
    private val RETURN_TYPE_HEAD = Regex("""\)\s*:\s*[\w.<>?, ]+\s*$""")
    private val FUNCTION_KEYWORD = Regex("""((?:\b(?:$MODIFIER_WORDS)\s+)*)\bfun\b""")
    // A class, object or interface header with nothing after it but its supertypes; `::class` is a
    // reference, not a declaration.
    private val TYPE_HEAD = Regex("""(?<!::)\b(?:class|object|interface)\b(?:(?!\bfun\b)[^{};=])*$""")
    private val PLAIN_BODIES = setOf("init", "get", "set", "constructor")
    private val IDENTIFIER_BEFORE = Regex("""(\w+)\s*(?:<[^<>]*>)?\s*$""")
    private val DECLARATION = Regex("""((?:\b(?:$MODIFIER_WORDS)\s+)*)\b(fun|val|var)\b""")

    /** Calls whose lambda runs in a coroutine, so code inside it can be cancelled. */
    private val COROUTINE_BUILDERS = setOf(
        "launch", "async", "withContext", "coroutineScope", "supervisorScope", "withTimeout", "withTimeoutOrNull",
        "runTest", "flow", "channelFlow", "callbackFlow", "transform", "collect", "collectLatest", "mapLatest",
        "flatMapLatest", "launchLatest", "launchExclusive", "LaunchedEffect", "produceState", "repeatOnLifecycle",
    )
}
