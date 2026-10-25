package com.base.app.buildlogic.guard

import com.base.app.buildlogic.guard.CodeRules.Location
import com.base.app.buildlogic.guard.CodeRules.Rule
// <opt:!ui_components>
import com.base.app.buildlogic.guard.CodeRules.packageLine
// </opt:!ui_components>
import com.base.app.buildlogic.guard.CodeRules.pattern

/**
 * Rules for the components in core:designsystem and the screens that arrange them: a component
 * sees only Compose and the theme, takes values and callbacks rather than state holders or data,
 * uses the modifier its caller passes, reads what moves in a layer, layout or draw lambda, and
 * describes what it shows or says it is decoration. Features and core:ui read type from the theme
 * and leave tree-wide values to it.
 */
internal object ComponentRules {

    // <opt:!ui_components>
    /**
     * The design system's components and primitives: its production code outside the theme (the
     * tokens), the window classes and the preview plumbing the project starts with.
     */
    val inDesignComponents: (Location) -> Boolean = { location ->
        location.inDesignSystem && location.isProduction &&
            KEPT_PLUMBING.none { it in location.path }
    }

    // </opt:!ui_components>
    private val designedCode: (Location) -> Boolean =
        { (it.inFeature || it.inDesignSystem) && it.isProduction }
    private val composeApi: (Location) -> Boolean =
        { (it.inFeature || it.inCoreUi || it.inDesignSystem || it.inNavigation) && it.isProduction }

    /** A state's value passed straight into a modifier that sizes, places, fades or turns. */
    private val COMPOSITION_READ = Regex(
        """\.\s*(alpha|scale|rotate|offset|padding|size|width|height|requiredSize)\s*\([^)]*\b\w+\.value\b""",
    )
    private val COMPOSITION_LOCAL = Regex(
        """(?<![\w.])(compositionLocalOf|staticCompositionLocalOf|compositionLocalWithComputedDefaultOf)\s*[(<{]""",
    )
    private val EMPTY_DESCRIPTION = Regex("""\bcontentDescription\s*=\s*""(?!")""")
    private val OFFSCREEN = Regex(
        """\bCompositingStrategy\s*\.\s*Offscreen\b|(?<![\w.])BlurEffect\s*\(|\bRenderEffect\b|(?<!\w)blur\s*\(""",
    )

    val ALL: List<Rule> = listOf(
        Rule(
            id = "designsystem-imports",
            advice = "the design system sees only Compose and its own tokens; take the value and " +
                "a callback, and let the route reach the data",
            applies = { it.inDesignSystem && it.isProduction },
            lines = { source, location -> foreignImports(source, location) },
        ),
        Rule(
            id = "stateless-params",
            advice = "a component takes values and callbacks, never a ViewModel, a repository " +
                "or a Flow; only a Route reaches for the ViewModel",
            applies = designedCode,
            lines = { source, location -> statefulParameters(source, location) },
        ),
        Rule(
            id = "raw-type",
            advice = "use a role from AppTheme.typography, with .copy() for a one-off",
            // <opt:ui_components>
            applies = { (it.inFeature || it.inCoreUi) && it.isProduction },
            // </opt:ui_components>
            // <opt-else:ui_components>
            //~ applies = {
            //~     ((it.inFeature || it.inCoreUi) && it.isProduction) || inDesignComponents(it)
            //~ },
            // </opt-else:ui_components>
            lines = pattern(
                Regex("""(?<![\w.])\d+(?:\.\d+)?[fF]?\s*\.\s*sp\b|(?<![\w.])TextStyle\s*\("""),
            ),
        ),
        Rule(
            id = "modifier-unused",
            advice = "apply the modifier the caller passes to what the composable draws, so the " +
                "caller can size, place and describe it",
            applies = composeApi,
            lines = { source, _ -> unusedModifiers(source) },
        ),
        Rule(
            id = "composition-read",
            advice = "read a moving value inside graphicsLayer { }, offset { } or layout { }, so a " +
                "change redraws or re-places rather than recomposes",
            applies = composeApi,
            lines = pattern(COMPOSITION_READ),
            reference = "jetpack-compose-performance/references/animation.md",
        ),
        Rule(
            id = "feature-composition-local",
            advice = "pass configuration as a parameter; a value the whole tree reads is a theme " +
                "token in core:designsystem",
            applies = { (it.inFeature || it.inCoreUi) && it.isProduction },
            lines = pattern(COMPOSITION_LOCAL),
        ),
        Rule(
            id = "empty-description",
            advice = "an empty description hides the element from screen readers; pass null for " +
                "decoration, or a string resource that says what it shows",
            applies = { it.isProduction },
            lines = pattern(EMPTY_DESCRIPTION),
        ),
        Rule(
            id = "feature-primitive",
            advice = "a feature arranges design-system components; draw it once, as a component " +
                "in core:designsystem",
            applies = { it.inFeature && it.isProduction },
            lines = pattern(
                Regex(
                    """\.\s*(background|border|drawBehind|drawWithContent|drawWithCache|shadow|""" +
                        """dropShadow|innerShadow)\s*[({]|""" +
                        """(?<![\w.])(Canvas|BasicText|BasicTextField|BasicSecureTextField)\s*\(""",
                ),
            ),
        ),
        // <opt:!ui_components>
        Rule(
            id = "component-preview",
            advice = "give the component a preview with @AppComponentPreviews, which the " +
                "screenshot tests render in both themes, at twice the font size, right to left " +
                "and narrow",
            applies = inDesignComponents,
            lines = { source, _ -> unpreviewedComponents(source) },
        ),
        // </opt:!ui_components>
        // An offscreen layer is sometimes the right tool (a mask, a blur behind floating chrome),
        // so the fix the rule asks for is a written reason, not a ban.
        Rule(
            id = "offscreen-layer",
            advice = "an offscreen layer renders its content to a texture on every frame it " +
                "changes; draw without it (CompositingStrategy.ModulateAlpha for alpha), or say " +
                "why this one pays for itself in the allow marker",
            applies = composeApi,
            lines = pattern(OFFSCREEN),
            reference = "jetpack-compose-performance/references/animation.md",
        ),
    )
    // <opt:!ui_components>

    /** Raw dp outside a `…Defaults` object, where a component's metrics belong; never 0 or 1. */
    fun rawDpOutsideDefaults(source: KotlinSource): List<Int> {
        val defaults = DEFAULTS_OBJECT.findAll(source.code).map { match ->
            val open = match.range.last
            source.lineOf(open)..source.lineOf(Declarations.closing(source.code, open))
        }.toList()
        return source.codeLines.mapIndexedNotNull { index, line ->
            val raw = RAW_DP.findAll(line).any { it.groupValues[1].toDouble() !in setOf(0.0, 1.0) }
            if (raw && defaults.none { index + 1 in it }) index + 1 else null
        }
    }
    // </opt:!ui_components>

    /** Imports of anything but Compose, the platform's drawing and the design system itself. */
    private fun foreignImports(source: KotlinSource, location: Location): List<Int> {
        val root = location.inSourceSet
            .substringAfter("kotlin/")
            .substringBefore("/core/designsystem/")
            .replace('/', '.')
        val own = "$root.core.designsystem."
        return source.codeLines.mapIndexedNotNull { index, line ->
            val imported = IMPORT.find(line)?.groupValues?.get(1) ?: return@mapIndexedNotNull null
            val project = imported.startsWith("$root.") && !imported.startsWith(own)
            if (project || FORBIDDEN_IMPORTS.containsMatchIn(imported)) index + 1 else null
        }
    }

    private fun statefulParameters(source: KotlinSource, location: Location): List<Int> {
        val parameters = Declarations.composables(source).flatMap { function ->
            function.parameters
                .filter { parameter ->
                    // A feature's ViewModel parameters are vm-forwarding's to report.
                    STATEFUL_TYPE.containsMatchIn(parameter.type) ||
                        (location.inDesignSystem && VIEW_MODEL.containsMatchIn(parameter.type))
                }
                .map { source.lineOf(it.offset) }
        }
        val calls = if (location.inFeature) viewModelsOutsideRoutes(source) else emptyList()
        return (parameters + calls).distinct().sorted()
    }

    /** A `hiltViewModel()` or `viewModel()` called in a composable that is not a Route. */
    private fun viewModelsOutsideRoutes(source: KotlinSource): List<Int> {
        val code = source.code
        val bodies = Declarations.composables(source).filterNot { it.name.endsWith("Route") }
        return bodies.flatMap { function ->
            val open = code.indexOf('{', code.indexOf(')', function.offset))
            if (open < 0) return@flatMap emptyList()
            val body = code.substring(open, Declarations.closing(code, open))
            VIEW_MODEL_CALL.findAll(body).map { source.lineOf(open + it.range.first) }.toList()
        }
    }

    /**
     * A composable's `modifier: Modifier` parameter its body never reads, reported on the
     * parameter's line. `modifier = …` as a call's named argument is not a read of it.
     */
    private fun unusedModifiers(source: KotlinSource): List<Int> =
        Declarations.composables(source).mapNotNull { function ->
            val parameter = function.parameters.firstOrNull { it.name == "modifier" && it.type == "Modifier" }
            val body = parameter?.let { Declarations.body(source, function) }
            if (body == null || MODIFIER_READ.containsMatchIn(source.code.substring(body.first, body.last + 1))) {
                null
            } else {
                source.lineOf(parameter.offset)
            }
        }.distinct().sorted()
    // <opt:!ui_components>

    /** A file declaring a component and no preview of it, reported on its package line. */
    private fun unpreviewedComponents(source: KotlinSource): List<Int> {
        val code = source.code
        val components = Declarations.composables(source).filter { function ->
            val head = code.substring(maxOf(0, function.offset - HEAD_LENGTH), function.offset)
            val takesModifier =
                function.parameters.any { it.name == "modifier" && it.type == "Modifier" }
            takesModifier && !PRIVATE_FUN.containsMatchIn(head)
        }
        val unpreviewed = components.isNotEmpty() && !PREVIEW.containsMatchIn(code)
        return if (unpreviewed) listOf(packageLine(source)) else emptyList()
    }

    private val KEPT_PLUMBING =
        listOf(
            "/designsystem/theme/",
            "/designsystem/foundation/adaptive/",
            "/designsystem/preview/",
        )
    // </opt:!ui_components>
    private val IMPORT = Regex("""^\s*import\s+([\w.]+)""")
    private val FORBIDDEN_IMPORTS = Regex(
        listOf(
            """androidx\.lifecycle\.viewmodel""", """androidx\.hilt""", """dagger\.""",
            """javax\.inject\.""", """io\.ktor\.""", """okhttp3\.""", """retrofit2\.""",
            """androidx\.room""", """androidx\.datastore""", """androidx\.paging""",
            """androidx\.work""", """coil3?\.""",
            """kotlinx\.coroutines\.flow\.(Mutable)?(State|Shared)Flow$""",
            """android\.content\.SharedPreferences$""", """java\.io\.File$""",
            """java\.net\.""", """android\.content\.Intent$""",
            """android\.widget\.Toast$""", """android\.util\.Log$""",
        ).joinToString("|", prefix = "^(", postfix = ")"),
    )
    private val STATEFUL_TYPE = Regex(
        """\b\w*(Repository|DataSource|Dao|UseCase)\b|""" +
            """\b(Mutable)?(StateFlow|SharedFlow|Flow)\s*<""",
    )
    private val VIEW_MODEL = Regex("""\b\w*ViewModel\b""")
    private val VIEW_MODEL_CALL =
        Regex("""(?<![\w.])(hiltViewModel|viewModel)\s*(<[^>]*>)?\s*\(""")

    /** `modifier` read: anything but the name of a call's named argument, `modifier = …`. */
    private val MODIFIER_READ = Regex("""(?<![\w.])modifier\b(?!\s*=(?!=))""")
    // <opt:!ui_components>
    private val RAW_DP = Regex("""(?<![\w.])(\d+(?:\.\d+)?)[fF]?\s*\.\s*dp\b""")
    private val DEFAULTS_OBJECT = Regex("""\bobject\s+\w+Defaults\s*\{""")
    private val PREVIEW = Regex("""@(Preview|AppComponentPreviews|AppScreenPreviews)\b""")
    private val PRIVATE_FUN = Regex("""\bprivate\s+(?:\w+\s+)*fun\s*$""")

    /** How far before a composable's name its modifiers are looked for. */
    private const val HEAD_LENGTH = 80
    // </opt:!ui_components>
}
