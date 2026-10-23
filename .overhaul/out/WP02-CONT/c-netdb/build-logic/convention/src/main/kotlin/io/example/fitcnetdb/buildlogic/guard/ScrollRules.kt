package io.example.fitcnetdb.buildlogic.guard

import io.example.fitcnetdb.buildlogic.guard.CodeRules.Location
import io.example.fitcnetdb.buildlogic.guard.CodeRules.Rule

/**
 * Scrolling that feels the same everywhere: every list, grid, pager and scrolling column decelerates
 * as an iOS scroll view does. Compose gives a container Android's spline fling unless it names
 * another, so a container that names none flings differently from its neighbours, and nothing in a
 * preview shows it.
 */
internal object ScrollRules {

    private val uiCode: (Location) -> Boolean = { (it.inFeature || it.inCoreUi || it.inApp) && it.isProduction }

    val ALL: List<Rule> = listOf(
        Rule(
            id = "scroll-fling",
            advice = "name the design system's fling: flingBehavior = AppScrollDefaults.flingBehavior() on a lazy " +
                "list, grid, verticalScroll or horizontalScroll, AppScrollDefaults.pagerFlingBehavior(state) on a pager",
            applies = uiCode,
            lines = { source, _ -> platformFlings(source) },
        ),
    )

    private val CONTAINER = Regex(
        """(?<![\w.])(?:LazyColumn|LazyRow|LazyVerticalGrid|LazyHorizontalGrid|LazyVerticalStaggeredGrid|""" +
            """LazyHorizontalStaggeredGrid|HorizontalPager|VerticalPager)\s*[({]""",
    )
    private val SCROLL_MODIFIER = Regex("""\.\s*(?:verticalScroll|horizontalScroll)\s*\(""")
    private val FLING_ARGUMENT = Regex("""\bflingBehavior\s*=""")
    private val NO_USER_SCROLL = Regex("""\buserScrollEnabled\s*=\s*false\b""")

    /**
     * Scrolling containers that name no fling. One the person cannot scroll (`userScrollEnabled = false`,
     * a placeholder list) never flings, so it passes.
     */
    private fun platformFlings(source: KotlinSource): List<Int> {
        val code = source.code
        return (CONTAINER.findAll(code) + SCROLL_MODIFIER.findAll(code)).filter { match ->
            val bracket = match.range.last
            if (code[bracket] == '{') return@filter true
            val arguments = code.substring(bracket, Declarations.closing(code, bracket))
            !FLING_ARGUMENT.containsMatchIn(arguments) && !NO_USER_SCROLL.containsMatchIn(arguments)
        }.map { source.lineOf(it.range.first) }.distinct().sorted().toList()
    }
}
