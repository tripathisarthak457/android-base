package com.example.everything.buildlogic.guard

import com.example.everything.buildlogic.guard.CodeRules.Location
import com.example.everything.buildlogic.guard.CodeRules.Rule
import com.example.everything.buildlogic.guard.CodeRules.pattern

/**
 * Rules that keep sheets on their one path: the sheet places itself, keeps clear of the keyboard,
 * draws its own shadow and moves only through its state. Each one names a mistake that compiles
 * and looks fine in a preview, and then stutters or clips on a phone.
 */
internal object SheetRules {

    private val sheetCode: (Location) -> Boolean = { it.isProduction && SHEET_PACKAGE in it.path }
    private val callers: (Location) -> Boolean =
        { (it.inFeature || it.inCoreUi || it.inDesignSystem || it.inApp || it.inNavigation) && it.isProduction }
    private val outsideDesignSystem: (Location) -> Boolean =
        { (it.inFeature || it.inCoreUi || it.inApp || it.inNavigation) && it.isProduction }

    val ALL: List<Rule> = listOf(
        Rule(
            id = "sheet-ime-padding",
            advice = "a sheet already keeps its content and footer above the keyboard; drop the IME padding " +
                "(it re-measures the content on every keyboard frame)",
            applies = callers,
            lines = { source, _ -> inSheetLambdas(source, IME_PADDING) },
        ),
        Rule(
            id = "sheet-shadow",
            advice = "a sheet draws its shadow from one cached nine-slice (SheetShadow); a shadow modifier " +
                "re-renders a blur every frame it moves",
            applies = sheetCode,
            lines = pattern(Regex("""\.\s*(?:shadow|dropShadow)\s*\(|\bshadowElevation\b""")),
        ),
        Rule(
            id = "sheet-raw-delta",
            advice = "move the sheet through its own drag and fling (one mutex, the rubber band, the release " +
                "rule); dispatchRawDelta clamps to the anchors and settle() is deprecated",
            applies = sheetCode,
            lines = pattern(Regex("""\bdispatchRawDelta\s*\(|\.\s*settle\s*\(""")),
        ),
        Rule(
            id = "sheet-window",
            advice = "sheets draw in the window's overlay layer, never in a Dialog or Popup window of their own",
            applies = sheetCode,
            lines = pattern(Regex("""(?<![\w.])(?:Dialog|Popup)\s*\(""")),
        ),
        Rule(
            id = "sheet-conditional",
            advice = "keep AppSheet composed and drive `visible` (or `item`): a sheet removed by an if, a when " +
                "or a lazy item vanishes without its exit",
            applies = callers,
            lines = { source, _ -> conditionalSheets(source) },
        ),
        Rule(
            id = "sheet-modifier",
            advice = "AppSheet's modifier is for test tags, semantics and focus; the sheet owns its size, " +
                "position, padding, clip, shadow and layers",
            applies = callers,
            lines = { source, _ -> sheetModifiers(source) },
        ),
        Rule(
            id = "sheet-root",
            advice = "give a sheet one root (a lazy list or SheetColumn) that takes contentPadding; the content " +
                "box stacks two at its top start",
            applies = callers,
            lines = { source, _ -> secondRoots(source) },
        ),
        Rule(
            id = "shell-seam",
            advice = "the shell seams are for the shell, navigation and liquid glass; a feature uses AppSheet",
            applies = { it.inFeature },
            lines = pattern(Regex("""\bShellSeamApi\b""")),
        ),
        Rule(
            id = "sheet-when-else",
            advice = "SheetDetent, SheetModality, SheetDismissSource and SheetMaterial gain values; give the " +
                "when an else branch",
            applies = outsideDesignSystem,
            lines = { source, _ -> whensWithoutElse(source) },
        ),
    )

    /** The lines of [regex] inside the arguments or trailing lambda of an AppSheet or a sheet slot provider. */
    private fun inSheetLambdas(source: KotlinSource, regex: Regex): List<Int> {
        val code = source.code
        return SHEET_CALLS.findAll(code).flatMap { call -> spanOf(code, call.range.last) }
            .flatMap { range -> regex.findAll(code.substring(range.first, range.last)).map { range.first + it.range.first } }
            .map(source::lineOf)
            .distinct()
            .sorted()
            .toList()
    }

    /** An `AppSheet(` whose innermost enclosing block is an if or else body, a when branch, or a lazy item. */
    private fun conditionalSheets(source: KotlinSource): List<Int> {
        val code = source.code
        return APP_SHEET.findAll(code).filter { call ->
            val start = call.range.first
            val statement = code.substring(statementStart(code, start), start)
            if (INLINE_CONDITION.containsMatchIn(statement)) return@filter true
            val brace = enclosingBrace(code, start) ?: return@filter false
            val head = code.substring(maxOf(0, brace - HEAD_LOOKBACK), brace).trimEnd()
            BLOCK_CONDITION.containsMatchIn(head) || isLazyItem(code, brace)
        }.map { source.lineOf(it.range.first) }.toList()
    }

    /** The `modifier = …` argument of each AppSheet, when it sizes, places, pads, clips or layers the sheet. */
    private fun sheetModifiers(source: KotlinSource): List<Int> {
        val code = source.code
        return APP_SHEET.findAll(code).mapNotNull { call ->
            val open = call.range.last
            val close = Declarations.closing(code, open)
            val arguments = code.substring(open + 1, close)
            val modifier = MODIFIER_ARGUMENT.find(arguments) ?: return@mapNotNull null
            val value = topLevelArgument(arguments, modifier.range.last + 1)
            if (FORBIDDEN_MODIFIER.containsMatchIn(value)) source.lineOf(open + 1 + modifier.range.first) else null
        }.toList()
    }

    /** The second composable emitted at the top of an AppSheet's trailing lambda. */
    private fun secondRoots(source: KotlinSource): List<Int> {
        val code = source.code
        return APP_SHEET.findAll(code).mapNotNull { call ->
            val close = Declarations.closing(code, call.range.last)
            val lambda = trailingLambda(code, close) ?: return@mapNotNull null
            val roots = topLevelCalls(code, lambda).filter { (name, _) -> name !in NOT_EMITTED }
            roots.getOrNull(1)?.let { (_, offset) -> source.lineOf(offset) }
        }.toList()
    }

    /** `when` blocks whose branches name a sheet type and that have no else. */
    private fun whensWithoutElse(source: KotlinSource): List<Int> {
        val code = source.code
        return WHEN.findAll(code).mapNotNull { match ->
            val brace = code.indexOf('{', match.range.last)
            if (brace < 0) return@mapNotNull null
            val body = code.substring(brace, Declarations.closing(code, brace))
            if (SHEET_TYPE_BRANCH.containsMatchIn(body) && !ELSE_BRANCH.containsMatchIn(body)) source.lineOf(match.range.first) else null
        }.toList()
    }

    /**
     * The arguments of the call whose parenthesis opens at [open], and its trailing lambda if any; or, for a call
     * made with a lambda alone (`ProvideSheetFooter { … }`), that lambda.
     */
    private fun spanOf(code: String, open: Int): Sequence<IntRange> {
        if (code[open] == '{') return sequenceOf((open + 1) until Declarations.closing(code, open))
        val close = Declarations.closing(code, open)
        val arguments = (open + 1) until close
        val lambda = trailingLambda(code, close)
        return if (lambda == null) sequenceOf(arguments) else sequenceOf(arguments, lambda)
    }

    /** The body of a lambda that follows the closing parenthesis at [close], as offsets inside its braces. */
    private fun trailingLambda(code: String, close: Int): IntRange? {
        var index = close + 1
        while (index < code.length && code[index].isWhitespace() && code[index] != '\n') index++
        if (index >= code.length || code[index] != '{') return null
        return (index + 1) until Declarations.closing(code, index)
    }

    /** Calls of capitalised names at brace depth zero inside [range], with their offsets. */
    private fun topLevelCalls(code: String, range: IntRange): List<Pair<String, Int>> {
        val calls = mutableListOf<Pair<String, Int>>()
        var depth = 0
        var parens = 0
        var index = range.first
        while (index < range.last) {
            val char = code[index]
            when {
                char == '{' -> depth++
                char == '}' -> depth--
                char == '(' -> parens++
                char == ')' -> parens--
                depth == 0 && parens == 0 && char.isUpperCase() && (index == 0 || !code[index - 1].isLetterOrDigit() && code[index - 1] != '.') -> {
                    val name = NAME.find(code, index)?.value.orEmpty()
                    val after = code.substring(index + name.length).trimStart()
                    if (after.startsWith("(") || after.startsWith("{")) calls += name to index
                    index += name.length - 1
                }
            }
            index++
        }
        return calls
    }

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

    private fun statementStart(code: String, offset: Int): Int {
        var index = offset - 1
        while (index >= 0 && code[index] != '\n' && code[index] != '{' && code[index] != ';') index--
        return index + 1
    }

    private fun isLazyItem(code: String, brace: Int): Boolean {
        var end = brace - 1
        while (end >= 0 && code[end].isWhitespace()) end--
        if (end >= 0 && code[end] == ')') {
            end = Declarations.opening(code, end) - 1
            while (end >= 0 && code[end].isWhitespace()) end--
        }
        var start = end
        while (start >= 0 && (code[start].isLetterOrDigit() || code[start] == '_')) start--
        return code.substring(start + 1, end + 1) in LAZY_ITEMS
    }

    private fun topLevelArgument(arguments: String, from: Int): String {
        var depth = 0
        for (index in from until arguments.length) {
            when (arguments[index]) {
                '(', '{', '[' -> depth++
                ')', '}', ']' -> depth--
                ',' -> if (depth == 0) return arguments.substring(from, index)
            }
        }
        return arguments.substring(from)
    }

    private const val SHEET_PACKAGE = "/component/sheet/"
    private const val HEAD_LOOKBACK = 160

    private val SHEET_CALLS = Regex("""(?<![\w.])(?:AppSheet|ProvideSheetHeader|ProvideSheetFooter)\s*[({]""")
    private val APP_SHEET = Regex("""(?<![\w.])AppSheet\s*\(""")
    private val IME_PADDING = Regex(
        """\bimePadding\s*\(|windowInsetsPadding\s*\(\s*WindowInsets\s*\.\s*ime\b|\bfitInside\s*\(\s*WindowInsetsRulers\s*\.\s*Ime""",
    )
    private val INLINE_CONDITION = Regex("""(?:\bif\s*\(.*\)|\belse|->)\s*$""")
    private val BLOCK_CONDITION = Regex("""(?:\bif\s*\(.*\)|\belse|->)$""")
    private val LAZY_ITEMS = setOf("item", "items", "itemsIndexed", "stickyHeader")
    private val MODIFIER_ARGUMENT = Regex("""\bmodifier\s*=""")
    private val FORBIDDEN_MODIFIER = Regex(
        """\.\s*(?:size|requiredSize|width|height|requiredWidth|requiredHeight|fillMax\w*|wrapContent\w*|sizeIn|widthIn|""" +
            """heightIn|offset|absoluteOffset|padding|absolutePadding|windowInsetsPadding|clip|clipToBounds|shadow|""" +
            """dropShadow|graphicsLayer|alpha|scale|rotate|zIndex)\s*[({]""",
    )
    private val NAME = Regex("""[A-Za-z_][A-Za-z0-9_]*""")

    /** Composables that emit nothing into the content box: effects, slot providers, guards and overlays. */
    private val NOT_EMITTED = setOf(
        "LaunchedEffect", "DisposableEffect", "SideEffect", "CompositionLocalProvider",
        "ProvideSheetHeader", "ProvideSheetFooter", "ProvideSheetStyle", "SheetBackHandler", "SheetDismissGuard",
        "SheetDismissConfirmation", "NavigationBackHandler", "AppSheet", "AppActionSheet", "AppChoiceSheet",
        "AppAlert", "AppLoadingOverlay", "AppMenu",
    )
    private val WHEN = Regex("""\bwhen\s*\(""")
    private val SHEET_TYPE_BRANCH = Regex("""(?:\bis\s+)?\b(?:SheetDetent|SheetModality|SheetDismissSource|SheetMaterial)\s*\.\s*\w+\s*(?:,|->)""")
    private val ELSE_BRANCH = Regex("""\belse\s*->""")
}
