package io.example.fitclocal.buildlogic.guard

/**
 * Item calls in lazy list, grid and staggered grid scopes. A call is in such a scope when one of
 * its enclosing blocks is the content lambda of a `Lazy…` call or the body of a `Lazy…Scope`
 * extension; it belongs to the innermost one.
 */
internal object LazyScopes {

    /** `items(…)` and `itemsIndexed(…)` over data that pass no `key`. */
    fun unkeyed(source: KotlinSource): List<Int> =
        calls(source)
            .filter { it.name == "items" || it.name == "itemsIndexed" }
            .filter { call -> call.arguments != null && !KEY.containsMatchIn(call.arguments) && !isCount(call.arguments) }
            .map { source.lineOf(it.offset) }
            .distinct()

    /** Item calls without a `contentType` in a scope that has two or more of them. */
    fun untyped(source: KotlinSource): List<Int> =
        calls(source)
            .groupBy { it.scope }
            .values
            .filter { it.size >= 2 }
            .flatten()
            .filter { call -> call.arguments == null || !CONTENT_TYPE.containsMatchIn(call.arguments) }
            .map { source.lineOf(it.offset) }
            .distinct()
            .sorted()

    private class Call(val name: String, val offset: Int, val arguments: String?, val scope: Int)

    private fun calls(source: KotlinSource): List<Call> {
        val code = source.code
        val found = CALL.findAll(code)
            .filterNot { FUN.containsMatchIn(code.substring(maxOf(0, it.range.first - 8), it.range.first)) }
            .toList()
        if (found.isEmpty()) return emptyList()

        // The innermost lazy block around each offset, as the offset of its opening brace.
        val scopeAt = IntArray(code.length) { -1 }
        val blocks = ArrayDeque<Int>()
        for (index in code.indices) {
            when (code[index]) {
                '{' -> blocks.addLast(if (isLazyBlock(code, index)) index else blocks.lastOrNull() ?: -1)
                '}' -> blocks.removeLastOrNull()
            }
            scopeAt[index] = blocks.lastOrNull() ?: -1
        }

        return found.mapNotNull { match ->
            val scope = scopeAt[match.range.first]
            if (scope < 0) return@mapNotNull null
            val bracket = match.range.last
            val arguments = if (code[bracket] == '(') code.substring(bracket + 1, Declarations.closing(code, bracket)) else null
            Call(match.groupValues[1], match.range.first, arguments, scope)
        }
    }

    /** `items(3)`, `items(PLACEHOLDER_ROWS)`, `items(count = n)`: rows with no identity but their index. */
    private fun isCount(arguments: String): Boolean {
        val first = arguments.substringBefore(',').trim()
        return COUNT.matches(first) || first.startsWith("count")
    }

    private fun isLazyBlock(code: String, brace: Int): Boolean {
        var end = brace - 1
        while (end >= 0 && code[end].isWhitespace()) end--
        if (end >= 0 && code[end] == ')') {
            end = Declarations.opening(code, end) - 1
            while (end >= 0 && code[end].isWhitespace()) end--
        }
        var start = end
        while (start >= 0 && (code[start].isLetterOrDigit() || code[start] == '_')) start--
        val callee = code.substring(start + 1, end + 1)
        val before = code.substring(maxOf(0, start - 80), start + 1)
        return LAZY_CALL.matches(callee) || LAZY_RECEIVER.containsMatchIn(before)
    }

    private val CALL = Regex("""(?<![\w.])(item|items|itemsIndexed|stickyHeader)\s*([({])""")
    private val LAZY_CALL = Regex("""^Lazy[A-Z]\w*$""")
    private val LAZY_RECEIVER = Regex("""Lazy\w*Scope\s*\.\s*$""")
    private val KEY = Regex("""\bkey\s*=""")
    private val CONTENT_TYPE = Regex("""\bcontentType\s*=""")
    private val COUNT = Regex("""^(?:\d+|[A-Z][A-Z0-9_]*)$""")
    private val FUN = Regex("""\bfun\s*$""")
}
