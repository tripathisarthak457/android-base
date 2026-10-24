package com.base.app.buildlogic.guard

import com.base.app.buildlogic.guard.CodeRules.Location
import com.base.app.buildlogic.guard.CodeRules.Rule
import com.base.app.buildlogic.guard.CodeRules.packageLine
import com.base.app.buildlogic.guard.CodeRules.pattern

/**
 * Rules for the components this project writes in core:designsystem, which it starts without: a
 * component sees only Compose and the theme, takes values and callbacks rather than state holders
 * or data, reads every colour, size, duration and text style from the tokens, and comes with a
 * preview the screenshot tests render. Features arrange those components and draw nothing of their
 * own.
 */
internal object ComponentRules {

    /**
     * The design system's components and primitives: its production code outside the theme (the
     * tokens), the window classes and the preview plumbing the project starts with.
     */
    val inDesignComponents: (Location) -> Boolean = { location ->
        location.inDesignSystem && location.isProduction &&
            KEPT_PLUMBING.none { it in location.path }
    }

    private val designedCode: (Location) -> Boolean =
        { (it.inFeature || it.inDesignSystem) && it.isProduction }

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
            applies = {
                ((it.inFeature || it.inCoreUi) && it.isProduction) || inDesignComponents(it)
            },
            lines = pattern(
                Regex("""(?<![\w.])\d+(?:\.\d+)?[fF]?\s*\.\s*sp\b|(?<![\w.])TextStyle\s*\("""),
            ),
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
        Rule(
            id = "component-preview",
            advice = "give the component a preview with @AppComponentPreviews, which the " +
                "screenshot tests render in both themes, at twice the font size, right to left " +
                "and narrow",
            applies = inDesignComponents,
            lines = { source, _ -> unpreviewedComponents(source) },
        ),
    )

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
    private val RAW_DP = Regex("""(?<![\w.])(\d+(?:\.\d+)?)[fF]?\s*\.\s*dp\b""")
    private val DEFAULTS_OBJECT = Regex("""\bobject\s+\w+Defaults\s*\{""")
    private val PREVIEW = Regex("""@(Preview|AppComponentPreviews|AppScreenPreviews)\b""")
    private val PRIVATE_FUN = Regex("""\bprivate\s+(?:\w+\s+)*fun\s*$""")

    /** How far before a composable's name its modifiers are looked for. */
    private const val HEAD_LENGTH = 80
}
