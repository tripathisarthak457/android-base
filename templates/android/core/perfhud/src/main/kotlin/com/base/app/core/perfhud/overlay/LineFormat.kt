package com.base.app.core.perfhud.overlay

/**
 * A line of the pill: a format string from resources whose arguments are whole numbers (`%1$d`,
 * `%2$d`) and whose only other conversion is `%%`, written into a buffer of its own.
 *
 * [write] puts the numbers in as ASCII digits, whatever the locale, as the log and the reports
 * write them, and allocates nothing: no formatter, no boxed argument, no string. The format is
 * parsed once; a conversion it does not support fails at that point, in the developer's build.
 */
internal class LineFormat(format: String) {

    // The text before each argument, and after the last one.
    private val literals: Array<CharArray>
    private val arguments: IntArray

    /** Holds the line written last, its first [length] chars. */
    val chars: CharArray

    var length: Int = 0
        private set

    init {
        val literals = ArrayList<CharArray>()
        val arguments = ArrayList<Int>()
        val literal = StringBuilder()
        var at = 0
        while (at < format.length) {
            val char = format[at]
            if (char != '%') {
                literal.append(char)
                at++
                continue
            }
            if (format.startsWith("%%", at)) {
                literal.append('%')
                at += 2
                continue
            }
            val dollar = format.indexOf('$', startIndex = at)
            val index = if (dollar >
                at + 1
            ) {
                format.substring(at + 1, dollar).toIntOrNull()
            } else {
                null
            }
            require(index != null && index >= 1 && format.getOrNull(dollar + 1) == 'd') {
                "Unsupported conversion at $at in \"$format\": only %n\$d and %% are."
            }
            literals += literal.toString().toCharArray()
            literal.clear()
            arguments += index - 1
            at = dollar + 2
        }
        literals += literal.toString().toCharArray()
        require(
            arguments.all {
                it < MaxArguments
            },
        ) { "At most $MaxArguments arguments: \"$format\"" }
        this.literals = literals.toTypedArray()
        this.arguments = arguments.toIntArray()
        chars = CharArray(literals.sumOf { it.size } + arguments.size * MaxIntChars)
    }

    /** Writes the line with its first argument [first] and its second [second]. */
    fun write(first: Int, second: Int = 0) {
        var end = 0
        for (index in arguments.indices) {
            end = put(literals[index], end)
            end = put(if (arguments[index] == 0) first else second, end)
        }
        length = put(literals[arguments.size], end)
    }

    private fun put(text: CharArray, at: Int): Int {
        text.copyInto(chars, destinationOffset = at)
        return at + text.size
    }

    private fun put(value: Int, at: Int): Int {
        // A Long, so that negating Int.MIN_VALUE cannot overflow.
        var rest = value.toLong()
        var start = at
        if (rest < 0) {
            chars[start++] = '-'
            rest = -rest
        }
        var digits = 1
        var probe = rest
        while (probe >= 10) {
            probe /= 10
            digits++
        }
        val end = start + digits
        var position = end - 1
        do {
            chars[position--] = '0' + (rest % 10).toInt()
            rest /= 10
        } while (rest > 0)
        return end
    }
}

private const val MaxArguments = 2

/** `-2147483648`. */
private const val MaxIntChars = 11
