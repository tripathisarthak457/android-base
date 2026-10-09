package com.base.app.buildlogic.guard

/**
 * A Kotlin source file with its comments and string literals blanked out, so a pattern search
 * only ever matches code. Blanking keeps every character's offset and every line break, so a
 * match in [code] has the same line number as in the file.
 *
 * Not a parser: it knows exactly enough of the lexical grammar to tell code from comment from
 * string, including nested block comments, raw strings and `${…}` templates (whose contents are
 * code again).
 */
internal class KotlinSource(val text: String) {

    /** Comments and string contents replaced by spaces; quotes and line breaks kept. */
    val code: String

    /** Comments replaced by spaces; strings kept, for rules that read what a string says. */
    val codeAndStrings: String

    /** Every `//` comment by line number (1-based), with the code before it on that line blanked or not. */
    val lineComments: Map<Int, LineComment>

    private val lineStarts: IntArray = text.indices.filter { text[it] == '\n' }.map { it + 1 }.let { intArrayOf(0) + it }

    init {
        val scan = Scanner(text).also { it.run() }
        code = String(scan.code)
        codeAndStrings = String(scan.codeAndStrings)
        lineComments = scan.comments.associate { (start, end) ->
            val line = lineOf(start)
            line to LineComment(
                text = text.substring(start, end),
                alone = code.substring(lineStarts[line - 1], start).isBlank(),
            )
        }
    }

    val codeLines: List<String> by lazy { code.split('\n') }
    val codeAndStringLines: List<String> by lazy { codeAndStrings.split('\n') }

    /** The 1-based line holding [offset]. */
    fun lineOf(offset: Int): Int {
        val index = lineStarts.binarySearch(offset)
        return if (index >= 0) index + 1 else -index - 1
    }

    data class LineComment(val text: String, val alone: Boolean)

    private class Scanner(private val text: String) {
        val code = text.toCharArray()
        val codeAndStrings = text.toCharArray()
        val comments = mutableListOf<Pair<Int, Int>>()

        private enum class Mode { CODE, STRING, RAW_STRING, TEMPLATE }

        /** A template's brace depth is kept with it, so `}` inside one closes the right thing. */
        private class Frame(val mode: Mode, var braces: Int = 0)

        private val stack = ArrayDeque<Frame>().apply { addLast(Frame(Mode.CODE)) }
        private var i = 0

        fun run() {
            while (i < text.length) {
                when (stack.last().mode) {
                    Mode.CODE, Mode.TEMPLATE -> code()
                    Mode.STRING -> string()
                    Mode.RAW_STRING -> rawString()
                }
            }
        }

        private fun code() {
            val c = text[i]
            val next = text.getOrNull(i + 1)
            val frame = stack.last()
            when {
                c == '/' && next == '/' -> {
                    val end = text.indexOf('\n', i).let { if (it < 0) text.length else it }
                    comments += i to end
                    blankComment(i, end)
                    i = end
                }
                c == '/' && next == '*' -> blockComment()
                text.startsWith("\"\"\"", i) -> {
                    stack.addLast(Frame(Mode.RAW_STRING))
                    i += 3
                }
                c == '"' -> {
                    stack.addLast(Frame(Mode.STRING))
                    i++
                }
                c == '\'' -> charLiteral()
                c == '`' -> {
                    // A backticked name can hold spaces and punctuation; none of it is code.
                    val end = text.indexOf('`', i + 1).let { if (it < 0) text.length - 1 else it }
                    blankString(i + 1, end)
                    i = end + 1
                }
                c == '{' && frame.mode == Mode.TEMPLATE -> {
                    frame.braces++
                    i++
                }
                c == '}' && frame.mode == Mode.TEMPLATE -> {
                    if (frame.braces == 0) {
                        stack.removeLast()
                        blankString(i, i + 1)
                    } else {
                        frame.braces--
                    }
                    i++
                }
                else -> i++
            }
        }

        private fun blockComment() {
            val start = i
            var depth = 0
            while (i < text.length) {
                when {
                    text.startsWith("/*", i) -> {
                        depth++
                        i += 2
                    }
                    text.startsWith("*/", i) -> {
                        depth--
                        i += 2
                        if (depth == 0) break
                    }
                    else -> i++
                }
            }
            blankComment(start, i)
        }

        private fun charLiteral() {
            var end = i + 1
            if (text.getOrNull(end) == '\\') end += 2 else end++
            while (end < text.length && text[end] != '\'' && text[end] != '\n') end++
            blankString(i + 1, end)
            i = end + 1
        }

        private fun string() {
            val c = text[i]
            when {
                c == '\\' -> {
                    blankString(i, minOf(i + 2, text.length))
                    i += 2
                }
                c == '"' -> {
                    stack.removeLast()
                    i++
                }
                c == '$' && text.getOrNull(i + 1) == '{' -> openTemplate()
                // An unterminated literal ends at the line, as the compiler would report it.
                c == '\n' -> {
                    stack.removeLast()
                    i++
                }
                else -> {
                    blankString(i, i + 1)
                    i++
                }
            }
        }

        private fun rawString() {
            val c = text[i]
            when {
                text.startsWith("\"\"\"", i) -> {
                    // Quotes right before the closing three belong to the string: `""""a""""`.
                    var end = i
                    while (end < text.length && text[end] == '"') end++
                    blankString(i, end - 3)
                    stack.removeLast()
                    i = end
                }
                c == '$' && text.getOrNull(i + 1) == '{' -> openTemplate()
                c == '\n' -> i++
                else -> {
                    blankString(i, i + 1)
                    i++
                }
            }
        }

        private fun openTemplate() {
            blankString(i, i + 2)
            stack.addLast(Frame(Mode.TEMPLATE))
            i += 2
        }

        private fun blankString(from: Int, to: Int) {
            for (k in from until to) if (code[k] != '\n') code[k] = ' '
        }

        private fun blankComment(from: Int, to: Int) {
            for (k in from until to) {
                if (code[k] != '\n') {
                    code[k] = ' '
                    codeAndStrings[k] = ' '
                }
            }
        }
    }
}
