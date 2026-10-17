package com.example.modulescompact.buildlogic.guard

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
