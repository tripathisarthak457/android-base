package com.base.app.buildlogic.guard

import com.base.app.buildlogic.guard.CodeRules.Location
import com.base.app.buildlogic.guard.CodeRules.Rule
import com.base.app.buildlogic.guard.CodeRules.anywhere
import com.base.app.buildlogic.guard.CodeRules.pattern

/** Rules that keep composition and drawing cheap, press feedback deliberate and Back in one system. */
internal object ComposeRules {

    private val uiCode: (Location) -> Boolean = { (it.inFeature || it.inCoreUi) && it.isProduction }
    private val designedCode: (Location) -> Boolean = { (it.inFeature || it.inDesignSystem) && it.isProduction }
    private val composeApi: (Location) -> Boolean =
        { (it.inFeature || it.inCoreUi || it.inDesignSystem) && it.isProduction }

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
            // The adaptive artifacts carry no Material components; adaptive-material-scope confines them.
            lines = pattern(
                Regex("""^\s*import\s+androidx\.compose\.material(?!\w*\.ripple\b)(?!3\.adaptive\.)"""),
                imports = true,
            ),
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
            applies = { it.inFeature && it.isProduction },
            lines = pattern(Regex("""\bNavDisplay\s*\.\s*(transitionSpec|popTransitionSpec|predictivePopTransitionSpec)\b""")),
        ),
        Rule(
            id = "collect-as-state",
            advice = "collectAsState keeps collecting in the background; use collectAsStateWithLifecycle()",
            applies = uiCode,
            lines = pattern(Regex("""\bcollectAsState\s*\(""")),
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
            applies = { it.inFeature && it.isProduction },
            lines = { source, _ -> rawDpLines(source) },
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
            lines = { source, _ -> LazyScopes.unkeyed(source) },
        ),
        Rule(
            id = "lazy-content-type",
            advice = "a lazy scope with more than one kind of item gives each a contentType, so rows are reused by kind",
            applies = uiCode,
            lines = { source, _ -> LazyScopes.untyped(source) },
        ),
        Rule(
            id = "state-param",
            advice = "take a () -> T provider instead of State<T>, so the read happens where it is needed",
            applies = composeApi,
            lines = { source, _ -> parameters(source) { _, parameter -> STATE_TYPE.containsMatchIn(parameter.type) } },
        ),
        Rule(
            id = "vm-forwarding",
            advice = "only a Route takes the ViewModel; pass the screen its state and an onEvent lambda",
            applies = { it.inFeature && it.isProduction },
            lines = { source, _ ->
                parameters(source) { function, parameter ->
                    !function.name.endsWith("Route") && VIEW_MODEL_TYPE.containsMatchIn(parameter.type)
                }
            },
        ),
        Rule(
            id = "animated-delegate",
            advice = "write val x = animate…(…) and read x.value inside a layout or draw lambda; `by` invites a read in composition",
            applies = designedCode,
            lines = pattern(Regex("""\bby\s+(?:[\w.]+\.)?animate\w*\s*(?:<[^>]*>)?\s*\(""")),
        ),
        Rule(
            id = "layer-alpha-fill",
            advice = "alpha on a full-screen layer renders it offscreen every frame; draw a scrim rect, or use " +
                "CompositingStrategy.ModulateAlpha",
            applies = designedCode,
            lines = { source, _ -> layerAlphaOnFill(source) },
        ),
        Rule(
            id = "legacy-back",
            advice = "handle Back with NavigationBackHandler (overlays at overlay priority), never the Activity's dispatcher",
            applies = { (it.inFeature || it.inCoreUi || it.inDesignSystem || it.inNavigation) && it.isProduction },
            lines = pattern(
                Regex("""(?<![\w.])(?:BackHandler|PredictiveBackHandler)\s*[({]|\bOnBackPressedCallback\b|\bonBackPressedDispatcher\b"""),
            ),
        ),
        Rule(
            id = "experimental-api",
            advice = "Style, Grid, FlexBox and MediaQuery are experimental here; use the stable equivalent " +
                "(LazyVerticalGrid, AppTheme.containerClass), or record the decision in the marker",
            applies = { true },
            lines = anywhere(
                Regex(
                    """\bandroidx\.compose\.foundation\.style\.|\b(?:ExperimentalFoundationStyleApi|ExperimentalGridApi|""" +
                        """ExperimentalFlexBoxApi|ExperimentalMediaQueryApi)\b""",
                ),
            ),
        ),
    )

    private val RAW_DP = Regex("""(?<![\w.])(\d+(?:\.\d+)?)[fF]?\s*\.\s*dp\b""")
    private val STATE_TYPE = Regex("""^(?:@\w+\s+)*(?:androidx\.compose\.runtime\.)?(?:Mutable)?State<""")
    private val VIEW_MODEL_TYPE = Regex("""^\w*ViewModel\b""")
    private val LAYER_ALPHA = Regex("""\.\s*(?:graphicsLayer|alpha)\s*[({]""")
    private val ALPHA = Regex("""\balpha\b""")
    private val FILL = Regex("""\bfillMaxSize\s*\(""")

    private fun rawDpLines(source: KotlinSource): List<Int> =
        source.codeLines.mapIndexedNotNull { index, line ->
            val raw = RAW_DP.findAll(line).any { it.groupValues[1].toDouble() !in setOf(0.0, 1.0) }
            if (raw) index + 1 else null
        }

    private fun parameters(
        source: KotlinSource,
        breaks: (Declarations.Function, Declarations.Parameter) -> Boolean,
    ): List<Int> =
        Declarations.composables(source).flatMap { function ->
            function.parameters.filter { breaks(function, it) }.map { source.lineOf(it.offset) }
        }.distinct()

    /**
     * `graphicsLayer { alpha = … }` or `.alpha(…)` in a modifier chain that also fills the screen.
     * A chain is the run of `.call(…)` links around the layer call.
     */
    private fun layerAlphaOnFill(source: KotlinSource): List<Int> {
        val code = source.code
        return LAYER_ALPHA.findAll(code).mapNotNull { match ->
            val bracket = match.range.last
            val end = Declarations.closing(code, bracket)
            val arguments = code.substring(bracket, end)
            val isAlpha = "alpha" in match.value || ALPHA.containsMatchIn(arguments)
            if (!isAlpha || "ModulateAlpha" in arguments) return@mapNotNull null
            val chain = code.substring(chainStart(code, match.range.first), chainEnd(code, end + 1))
            if (FILL.containsMatchIn(chain)) source.lineOf(match.range.first) else null
        }.distinct().toList()
    }

    private fun chainStart(code: String, dot: Int): Int {
        var index = dot
        while (true) {
            var end = index - 1
            while (end >= 0 && code[end].isWhitespace()) end--
            if (end >= 0 && (code[end] == ')' || code[end] == '}')) end = Declarations.opening(code, end) - 1
            while (end >= 0 && code[end].isWhitespace()) end--
            var start = end
            while (start >= 0 && (code[start].isLetterOrDigit() || code[start] == '_')) start--
            var before = start
            while (before >= 0 && code[before].isWhitespace()) before--
            if (before < 0 || code[before] != '.') return start + 1
            index = before
        }
    }

    private fun chainEnd(code: String, from: Int): Int {
        var index = from
        while (true) {
            var next = index
            while (next < code.length && code[next].isWhitespace()) next++
            if (next >= code.length || code[next] != '.') return index
            next++
            while (next < code.length && (code[next].isLetterOrDigit() || code[next] == '_' || code[next].isWhitespace())) next++
            while (next < code.length && (code[next] == '(' || code[next] == '{')) {
                next = Declarations.closing(code, next) + 1
                while (next < code.length && code[next] == ' ') next++
            }
            index = next
        }
    }
}
