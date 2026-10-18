package com.base.app.buildlogic.guard

import com.base.app.buildlogic.guard.CodeRules.Location
import com.base.app.buildlogic.guard.CodeRules.Rule
import com.base.app.buildlogic.guard.CodeRules.anywhere
import com.base.app.buildlogic.guard.CodeRules.pattern

/**
 * Rules that keep every screen working on every window: phones either way up, foldables, tablets,
 * desktop windows of any size (ADAPTIVE-PLAN 11.1). Layout is decided from the window's size
 * class and posture, never from the device or its orientation.
 */
internal object AdaptiveRules {

    private val uiModules: (Location) -> Boolean =
        { (it.inFeature || it.inCoreUi || it.inDesignSystem || it.inNavigation) && it.isProduction }

    val ALL: List<Rule> = listOf(
        Rule(
            id = "adaptive-orientation",
            advice = "a window's orientation says nothing about its size; use AppTheme.windowClass or containerClass",
            applies = { it.isProduction },
            lines = pattern(
                Regex(
                    """\b[cC]onfiguration(?:\.current)?\.orientation\b|\bORIENTATION_(?:LANDSCAPE|PORTRAIT)\b|""" +
                        """\b(?:set|get)RequestedOrientation\b|\brequestedOrientation\b""",
                ),
            ),
        ),
        Rule(
            id = "adaptive-screen-metrics",
            advice = "the screen is not the window; use containerClass, the layout's constraints, or the window " +
                "metrics inside a layout lambda",
            applies = { it.isProduction && !(it.inDesignSystem && ADAPTIVE_FOUNDATION in it.path) },
            lines = pattern(
                Regex(
                    """\bscreen(?:Width|Height)Dp\b|\bsmallestScreenWidthDp\b|\bdisplayMetrics\s*\.\s*(?:width|height)Pixels\b|""" +
                        """\bdefaultDisplay\b|\bgetRealMetrics\b|\b(?:current|maximum)WindowMetrics\b|\bLocalWindowInfo\b""",
                ),
            ),
        ),
        Rule(
            id = "adaptive-device-type",
            advice = "a phone can have a tablet-sized window and the reverse; branch on the window's classes",
            applies = uiModules,
            lines = pattern(Regex("""\bisTablet\b|\bisPhone\b|\bR\.bool\.is_tablet\b|\bBuild\s*\.\s*(?:MODEL|DEVICE|PRODUCT)\b""")),
        ),
        Rule(
            id = "adaptive-fixed-size",
            advice = "a width or height this large breaks a narrow or short window; use widthIn(max = AppTheme.layout.…), " +
                "a weight or fillMax*",
            applies = { location ->
                location.isProduction &&
                    (location.inCoreUi || location.module == "catalog" || (location.inDesignSystem && COMPONENT in location.path))
            },
            lines = pattern(
                Regex(
                    """\.\s*(?:width|requiredWidth|size|requiredSize)\(\s*(?:[3-9]\d\d|\d{4,})(?:\.\d+)?\s*\.\s*dp\b|""" +
                        """\.\s*(?:height|requiredHeight)\(\s*(?:4[89]\d|[5-9]\d\d|\d{4,})(?:\.\d+)?\s*\.\s*dp\b""",
                ),
            ),
        ),
        Rule(
            id = "adaptive-box-constraints",
            advice = "BoxWithConstraints subcomposes and costs a frame; use containerClass or a custom Layout",
            applies = { it.inFeature && it.isProduction },
            lines = pattern(Regex("""\bBoxWithConstraints\b""")),
            reference = "jetpack-compose-performance/references/layouts.md",
        ),
        Rule(
            id = "adaptive-mediaquery",
            advice = "MediaQuery is experimental; read AppTheme.input and AppTheme.posture",
            applies = { true },
            lines = anywhere(Regex("""\bisMediaQueryIntegrationEnabled\b|\bmediaQuery\s*\{|\bLocalUiMediaScope\b""")),
        ),
        Rule(
            id = "adaptive-material-scope",
            advice = "Material's adaptive types stay behind NavPane and AppShell; NavigationSuiteScaffold needs Material " +
                "components, so the shell's bar, rail and sidebar take its place",
            applies = { true },
            lines = { source, location -> materialScope(source, location) },
        ),
        Rule(
            id = "adaptive-preview",
            advice = "annotate a preview of this screen with @AppScreenPreviews, so it is seen at every window size",
            applies = { it.inFeature && it.isProduction },
            lines = { source, _ -> unpreviewedScreens(source) },
        ),
    )

    private const val ADAPTIVE_FOUNDATION = "/foundation/adaptive/"
    private const val COMPONENT = "/component/"
    private const val SCENE = "/scene/"

    private val NAVIGATION_SUITE = Regex("""\bandroidx\.compose\.material3\.adaptive\.navigationsuite\b|\bNavigationSuiteScaffold\b""")
    private val ADAPTIVE_IMPORT = Regex("""^\s*import\s+androidx\.compose\.material3\.adaptive\.""")
    private val SCREEN = Regex("""^\s*(?:@[\w.]+\s+)*(?:(?:internal|public|private)\s+)?fun\s+(\w+Screen)\s*\(""")
    private val SCREEN_PREVIEWS = Regex("""@AppScreenPreviews\b""")

    private fun materialScope(source: KotlinSource, location: Location): List<Int> {
        val allowed = (location.inNavigation && SCENE in location.path) ||
            (location.inDesignSystem && ADAPTIVE_FOUNDATION in location.path)
        return source.codeLines.mapIndexedNotNull { index, line ->
            val broken = NAVIGATION_SUITE.containsMatchIn(line) || (!allowed && ADAPTIVE_IMPORT.containsMatchIn(line))
            if (broken) index + 1 else null
        }
    }

    private fun unpreviewedScreens(source: KotlinSource): List<Int> {
        if (SCREEN_PREVIEWS.containsMatchIn(source.code)) return emptyList()
        val depths = source.depths
        return source.codeLines.mapIndexedNotNull { index, line ->
            val start = source.offsetOf(index + 1)
            if (depths.braces[start] == 0 && SCREEN.containsMatchIn(line)) index + 1 else null
        }
    }
}
