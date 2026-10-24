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
            // <opt:ui_components>
            advice = "no ripple or grey overlay; use Modifier.pressable(onClick, feedback = PressFeedback.…)",
            // </opt:ui_components>
            // <opt-else:ui_components>
            //~ advice = "no ripple or grey overlay; give the control the press feedback its component in core:designsystem " +
            //~     "defines (docs/DESIGN-SYSTEM.md)",
            // </opt-else:ui_components>
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
            reference = "jetpack-compose-performance/references/android/develop/ui/compose/custom-modifiers.md",
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
            reference = "jetpack-compose-performance/references/effects-and-threading.md",
        ),
        Rule(
            id = "raw-tween",
            advice = "use a spring from AppTheme.springs, which follows the motion style and reduce motion",
            // <opt:ui_components>
            applies = uiCode,
            // </opt:ui_components>
            // <opt-else:ui_components>
            //~ applies = { uiCode(it) || ComponentRules.inDesignComponents(it) },
            // </opt-else:ui_components>
            lines = pattern(Regex("""(?<![\w.])(tween|spring)\s*(<[^>]*>)?\s*\(""")),
        ),
        Rule(
            id = "raw-color",
            advice = "use a colour role from AppTheme.colors, which follows dark mode and the brand",
            // <opt:ui_components>
            applies = uiCode,
            // </opt:ui_components>
            // <opt-else:ui_components>
            //~ applies = { uiCode(it) || ComponentRules.inDesignComponents(it) },
            // </opt-else:ui_components>
            lines = pattern(Regex("""\bColor\s*\(\s*0[xX]""")),
        ),
        Rule(
            id = "raw-dp",
            advice = "use AppTheme.spacing or a size token; only 0.dp and 1.dp are written raw",
            // <opt:ui_components>
            applies = { it.inFeature && it.isProduction },
            lines = { source, _ -> rawDpLines(source) },
            // </opt:ui_components>
            // <opt-else:ui_components>
            //~ applies = { (it.inFeature && it.isProduction) || ComponentRules.inDesignComponents(it) },
            //~ // A component's metrics are written once, in its …Defaults object.
            //~ lines = { source, location ->
            //~     if (ComponentRules.inDesignComponents(location)) {
            //~         ComponentRules.rawDpOutsideDefaults(source)
            //~     } else {
            //~         rawDpLines(source)
            //~     }
            //~ },
            // </opt-else:ui_components>
        ),
        Rule(
            id = "subcompose-image",
            advice = "SubcomposeAsyncImage composes per load state and stutters in lists; use AsyncImage",
            applies = uiCode,
            lines = pattern(Regex("""\bSubcomposeAsyncImage\b""")),
            reference = "jetpack-compose-performance/references/layouts.md",
        ),
        Rule(
            id = "bare-clickable",
            // <opt:ui_components>
            advice = "say which press feedback a control uses: Modifier.pressable(onClick, feedback = PressFeedback.…)",
            // </opt:ui_components>
            // <opt-else:ui_components>
            //~ advice = "say which press feedback a control uses: build it as a component, or a press modifier, in " +
            //~     "core:designsystem (docs/DESIGN-SYSTEM.md)",
            // </opt-else:ui_components>
            applies = uiCode,
            lines = pattern(Regex("""\.\s*(clickable|combinedClickable)\s*[({]""")),
        ),
        Rule(
            id = "lazy-key",
            advice = "give lazy rows a stable key = { … } so they keep their state and the scroll position",
            applies = uiCode,
            lines = { source, _ -> LazyScopes.unkeyed(source) },
            reference = "jetpack-compose-performance/references/lazy-layouts.md",
        ),
        Rule(
            id = "lazy-content-type",
            advice = "a lazy scope with more than one kind of item gives each a contentType, so rows are reused by kind",
            applies = uiCode,
            lines = { source, _ -> LazyScopes.untyped(source) },
            reference = "jetpack-compose-performance/references/lazy-layouts.md",
        ),
        Rule(
            id = "scroll-chrome",
            // <opt:ui_components>
            advice = "pad scrolling content inside the scroll, so it runs under the bars and rests clear of them: " +
                "contentPadding = chromeContentPadding(…) on a lazy list, " +
                ".verticalScroll(…).padding(chromeContentPadding(…)) on a column",
            // </opt:ui_components>
            // <opt-else:ui_components>
            //~ advice = "pad scrolling content inside the scroll, so it runs under the system bars and " +
            //~     "rests clear of them: contentPadding = WindowInsets.safeDrawing.asPaddingValues() on " +
            //~     "a lazy list, .verticalScroll(…).windowInsetsPadding(WindowInsets.safeDrawing) on a " +
            //~     "column, or the contentPadding a shell of yours hands the screen",
            // </opt-else:ui_components>
            applies = { it.inFeature && it.isProduction },
            lines = { source, _ -> unpaddedScrolls(source) },
        ),
        Rule(
            id = "state-param",
            advice = "take a () -> T provider instead of State<T>, so the read happens where it is needed",
            applies = composeApi,
            lines = { source, _ -> parameters(source) { _, parameter -> STATE_TYPE.containsMatchIn(parameter.type) } },
            reference = "jetpack-compose-performance/references/state-and-stability.md",
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
            reference = "jetpack-compose-performance/references/animation.md",
        ),
        // No whole screen is rendered offscreen, with one exception outside this rule's modules: the back
        // gesture's card in :core:navigation draws the screen under it from one texture, rendered once per
        // gesture while that screen is still and released when the card is gone. BackCardState.textures
        // says why that is allowed and which tests hold it to those conditions.
        Rule(
            id = "layer-alpha-fill",
            advice = "alpha on a full-screen layer renders it offscreen every frame; draw a scrim rect, or use " +
                "CompositingStrategy.ModulateAlpha",
            applies = designedCode,
            lines = { source, _ -> layerAlphaOnFill(source) },
            reference = "jetpack-compose-performance/references/animation.md",
        ),
        Rule(
            id = "pixel-line",
            // <opt:ui_components>
            advice = "draw a separator one pixel thick as a filled rect, with Modifier.hairline (drawHairline inside " +
                "the design system); a one-pixel drawLine costs the GPU far more in every frame its screen moves",
            // </opt:ui_components>
            // <opt-else:ui_components>
            //~ advice = "draw a separator one pixel thick as a filled rect, drawRect over a one-pixel height; a " +
            //~     "one-pixel drawLine costs the GPU far more in every frame its screen moves",
            // </opt-else:ui_components>
            applies = { (it.inFeature || it.inCoreUi || it.inDesignSystem || it.inNavigation || it.inApp) && it.isProduction },
            lines = { source, _ -> onePixelLines(source) },
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

    private val SCREEN_LIST = Regex("""(?<![\w.])(?:LazyColumn|LazyVerticalGrid|LazyVerticalStaggeredGrid)\s*[({]""")
    // <opt:ui_components>
    private val CHROME_CONTENT_PADDING = Regex("""\bcontentPadding\s*=\s*(?:chromeContentPadding\s*\(|contentPadding\b)""")
    // </opt:ui_components>
    // <opt-else:ui_components>
    //~ // Until the app has a shell, the chrome is the system bars: padding from their insets, or the
    //~ // contentPadding a shell hands the screen once there is one.
    //~ private const val INSETS =
    //~     """WindowInsets\s*\.\s*\w+(?:\s*\.\s*\w+\s*\([^()]*\))*\s*\.\s*asPaddingValues\s*\("""
    //~ private val CHROME_CONTENT_PADDING = Regex("""\bcontentPadding\s*=\s*(?:contentPadding\b|$INSETS)""")
    // </opt-else:ui_components>
    private val VERTICAL_SCROLL = Regex("""\.\s*verticalScroll\s*\(""")
    // <opt:ui_components>
    private val CHROME_PADDING = Regex("""\.\s*padding\s*\(\s*(?:chromeContentPadding\s*\(|contentPadding\s*\))""")
    // </opt:ui_components>
    // <opt-else:ui_components>
    //~ private val CHROME_PADDING = Regex(
    //~     """\.\s*(?:padding\s*\(\s*(?:contentPadding\s*\)|$INSETS)|windowInsetsPadding\s*\(|""" +
    //~         """safe(?:Drawing|Content)Padding\s*\(|systemBarsPadding\s*\()""",
    //~ )
    // </opt-else:ui_components>
    private val RAW_DP = Regex("""(?<![\w.])(\d+(?:\.\d+)?)[fF]?\s*\.\s*dp\b""")
    private val STATE_TYPE = Regex("""^(?:@\w+\s+)*(?:androidx\.compose\.runtime\.)?(?:Mutable)?State<""")
    private val VIEW_MODEL_TYPE = Regex("""^\w*ViewModel\b""")
    private val LAYER_ALPHA = Regex("""\.\s*(?:graphicsLayer|alpha)\s*[({]""")
    private val ALPHA = Regex("""\balpha\b""")
    private val FILL = Regex("""\bfillMaxSize\s*\(""")
    private val DRAW_LINE = Regex("""(?<![\w.])drawLine\s*\(""")
    private val ONE_PIXEL_STROKE = Regex("""^strokeWidth\s*=\s*1f$""")

    /** `drawLine` calls one pixel thick: `strokeWidth = 1f` by name, or `1f` as the fourth argument. */
    private fun onePixelLines(source: KotlinSource): List<Int> {
        val code = source.code
        return DRAW_LINE.findAll(code).filter { match ->
            val open = match.range.last
            val arguments = topLevelArguments(code.substring(open + 1, Declarations.closing(code, open)))
            arguments.any { ONE_PIXEL_STROKE.matches(it) } || arguments.getOrNull(3) == "1f"
        }.map { source.lineOf(it.range.first) }.toList()
    }

    /** The comma-separated arguments of a call, trimmed, not split inside nested brackets. */
    private fun topLevelArguments(inside: String): List<String> {
        val arguments = mutableListOf<String>()
        var depth = 0
        var start = 0
        inside.forEachIndexed { index, char ->
            when (char) {
                '(', '[', '{' -> depth++
                ')', ']', '}' -> depth--
                ',' -> if (depth == 0) {
                    arguments += inside.substring(start, index).trim()
                    start = index + 1
                }
            }
        }
        inside.substring(start).trim().takeIf { it.isNotEmpty() }?.let { arguments += it }
        return arguments
    }

    private fun rawDpLines(source: KotlinSource): List<Int> =
        source.codeLines.mapIndexedNotNull { index, line ->
            val raw = RAW_DP.findAll(line).any { it.groupValues[1].toDouble() !in setOf(0.0, 1.0) }
            if (raw) index + 1 else null
        }

    /**
     * Scrolling containers whose content the chrome does not pad: a lazy list without
     * `contentPadding = chromeContentPadding(…)`, or a `verticalScroll` whose modifier chain does not
     * go on to `.padding(chromeContentPadding(…))`. Padding outside the scroll would clip the content
     * at the bars rather than let it run under them. A container that takes its caller's
     * `contentPadding` leaves the chrome to that caller.
     */
    private fun unpaddedScrolls(source: KotlinSource): List<Int> {
        val code = source.code
        val lists = SCREEN_LIST.findAll(code).filter { match ->
            val bracket = match.range.last
            code[bracket] == '{' ||
                !CHROME_CONTENT_PADDING.containsMatchIn(code.substring(bracket, Declarations.closing(code, bracket)))
        }
        val columns = VERTICAL_SCROLL.findAll(code).filter { match ->
            val after = minOf(Declarations.closing(code, match.range.last) + 1, code.length)
            !CHROME_PADDING.containsMatchIn(code.substring(after, minOf(chainEnd(code, after), code.length)))
        }
        return (lists + columns).map { source.lineOf(it.range.first) }.distinct().sorted().toList()
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
