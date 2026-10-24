package io.example.fitclocal.adaptive

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import io.example.fitclocal.core.designsystem.foundation.layout.ChromeInset
import io.example.fitclocal.core.designsystem.foundation.layout.LocalBottomChrome
import io.example.fitclocal.core.designsystem.preview.AppScreenPreviews
import io.example.fitclocal.core.designsystem.theme.AppTheme
import io.example.fitclocal.core.designsystem.theme.PlatformFonts
import io.example.fitclocal.core.testing.compose.TEST_SDK
import io.example.fitclocal.core.testing.compose.adaptive.WindowSize
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import sergio.sastre.composable.preview.scanner.android.AndroidComposablePreviewScanner
import sergio.sastre.composable.preview.scanner.android.AndroidPreviewInfo
import sergio.sastre.composable.preview.scanner.core.preview.ComposablePreview

/**
 * Every screen whose preview carries [AppScreenPreviews], rendered at each window a pull request is
 * checked at and at font scales 1 and 2, held to what a person would see as broken at that size,
 * whatever the design: text cut off, a control out of the window or under the floating tab bar, a
 * control too small to hit without touching another, two controls on top of each other, lines too
 * long to read on a wide window.
 *
 * It needs no goldens and runs with every unit test, so a screen that only works on a phone fails
 * here before anyone ships it. A screen joins by putting `@AppScreenPreviews` on its stateless
 * preview; `verifyCodeRules` (`adaptive-preview`) asks every screen to. A failure names the
 * screen, the window, the font scale and each node at fault: fix the layout, never the audit.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [TEST_SDK], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AdaptiveAuditTest(private val case: AuditCase) {

    private val compose = createComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(WindowRule(case.window)).around(compose)

    @Test
    fun `the screen fits the window`() {
        val preview = case.preview
        assumeTrue("no screen in this project carries @AppScreenPreviews", preview != null)
        checkNotNull(preview)
        var readable = Dp.Unspecified
        compose.setContent {
            AppTheme(fonts = PlatformFonts) {
                // The shell writes the tab bar's height here; a preview has no shell, so the audit
                // stands in for it wherever the shell shows the bar. A holder of its own, set before
                // the first measure, so no case inherits another's bar through the shared default.
                val tabBar = remember { ChromeInset().apply { value = case.window.tabBar } }
                readable = AppTheme.layout.readableMaxWidth + AppTheme.spacing.gutter
                CompositionLocalProvider(LocalBottomChrome provides tabBar) {
                    DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(case.fontScale)) {
                        preview()
                    }
                }
            }
        }
        compose.waitForIdle()

        val root = compose.onRoot(useUnmergedTree = true).fetchSemanticsNode()
        val problems = Audit(root, root.layoutInfo.density, readable, case.window).problems()
        assertTrue("$case:\n${problems.joinToString("\n") { "  $it" }}", problems.isEmpty())
    }

    /** One screen at one window and one font scale. */
    class AuditCase(
        val preview: ComposablePreview<AndroidPreviewInfo>?,
        val window: WindowSize,
        val fontScale: Float,
    ) {
        override fun toString(): String =
            if (preview == null) "no screen" else "${preview.methodName} at $window, font ${fontScale}x"
    }

    companion object {
        /** The project's package, renamed with it, so the scan sees every feature and nothing else. */
        private const val APP_PACKAGE = "io.example.fitclocal"

        private val FONT_SCALES = listOf(1f, 2f)

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> {
            // Each @Preview inside the annotation is a preview of its own; the audit sets the window
            // and the font itself, so one per function is enough.
            val screens = AndroidComposablePreviewScanner()
                .scanPackageTrees(APP_PACKAGE)
                .includeIfAnnotatedWithAnyOf(AppScreenPreviews::class.java)
                .includePrivatePreviews()
                .getPreviews()
                .distinctBy { "${it.declaringClass}.${it.methodName}" }
            if (screens.isEmpty()) return listOf(arrayOf(AuditCase(null, WindowSize.Phone, 1f)))
            return screens.flatMap { screen ->
                WindowSize.entries.flatMap { window ->
                    FONT_SCALES.map { scale -> arrayOf<Any>(AuditCase(screen, window, scale)) }
                }
            }
        }
    }
}

/**
 * What the shell keeps over the bottom edge: a floating tab bar with its margin, only where the
 * shell shows a bar (compact widths; a rail or a sidebar takes the side of anything wider).
 */
private val WindowSize.tabBar: Dp get() = if (width < 600.dp) 96.dp else 0.dp

/** Sizes the window before the compose rule starts its activity, as a device of that size would. */
private class WindowRule(private val window: WindowSize) : TestRule {
    override fun apply(base: Statement, description: Description): Statement = object : Statement() {
        override fun evaluate() {
            val orientation = if (window.width > window.height) "land" else "port"
            RuntimeEnvironment.setQualifiers(
                "w${window.width.value.toInt()}dp-h${window.height.value.toInt()}dp-$orientation",
            )
            base.evaluate()
        }
    }
}

private class Audit(
    private val root: SemanticsNode,
    private val density: Density,
    private val readable: Dp,
    private val window: WindowSize,
) {
    private val nodes = root.descendants().toList()
    private val bounds = root.boundsInRoot

    fun problems(): List<String> = textProblems() + controlProblems() + overlaps()

    private fun textProblems(): List<String> = nodes.flatMap { node ->
        node.textLayouts().flatMap { text ->
            buildList {
                // A text that may wrap without limit has no reason to be cut; one with maxLines
                // chose its ellipsis.
                if (text.layoutInput.maxLines == Int.MAX_VALUE && text.hasVisualOverflow) {
                    add("${node.label()}: the text is cut off")
                }
                if (window.width >= WIDE) {
                    val longest = (0 until text.lineCount).maxOfOrNull { text.getLineRight(it) - text.getLineLeft(it) }
                    if (longest != null && longest.asDp() > readable) {
                        add("${node.label()}: a line ${longest.asDp()} wide, past the readable ${readable}")
                    }
                }
            }
        }
    }

    private fun controlProblems(): List<String> = clickables().flatMap { node ->
        buildList {
            val box = Rect(node.positionInRoot, node.size.toSize())
            // Compose answers a touch up to 48 dp around a smaller control, which only works while
            // that area is the control's alone. Sizes are the laid-out ones: a list row cut by the
            // window's edge is no smaller a target for it.
            val touch = box.atLeast(with(density) { MIN_TARGET.toPx() })
            if (touch != box && !node.boundsInRoot.isEmpty) {
                clickables().firstOrNull { other ->
                    other.id != node.id && !node.isAncestorOf(other) && !other.isAncestorOf(node) &&
                        touch.overlaps(other.boundsInRoot) && !box.overlaps(other.boundsInRoot)
                }?.let { other -> add("${node.label()}: its $MIN_TARGET touch area runs into ${other.label()}") }
            }
            // Rows of a scrolling list run past the window and under the bar by design, and come to
            // rest clear of them through the list's content padding.
            if (!node.scrolls()) {
                if (box.left < bounds.left - EDGE || box.top < bounds.top - EDGE ||
                    box.right > bounds.right + EDGE || box.bottom > bounds.bottom + EDGE
                ) {
                    add("${node.label()}: partly outside the window at $box")
                }
                val bar = with(density) { window.tabBar.toPx() }
                if (bar > 0f && box.bottom > bounds.bottom - bar + EDGE) {
                    add("${node.label()}: under the tab bar")
                }
            }
        }
    }

    private fun overlaps(): List<String> {
        val controls = clickables().filter { it.boundsInRoot.width > EDGE && it.boundsInRoot.height > EDGE }
        return controls.flatMapIndexed { index, first ->
            controls.drop(index + 1).mapNotNull { second ->
                val shared = first.boundsInRoot.intersect(second.boundsInRoot)
                val nested = first.isAncestorOf(second) || second.isAncestorOf(first)
                if (!nested && shared.width > EDGE && shared.height > EDGE) {
                    "${first.label()} and ${second.label()} overlap"
                } else {
                    null
                }
            }
        }
    }

    private fun clickables(): List<SemanticsNode> = nodes.filter { SemanticsActions.OnClick in it.config }

    private fun Float.asDp(): Dp = with(density) { this@asDp.toDp() }
}

/** This rectangle grown about its centre to at least [size] each way. */
private fun Rect.atLeast(size: Float): Rect {
    val dx = ((size - width) / 2).coerceAtLeast(0f)
    val dy = ((size - height) / 2).coerceAtLeast(0f)
    return Rect(left - dx, top - dy, right + dx, bottom + dy)
}

private fun SemanticsNode.descendants(): Sequence<SemanticsNode> =
    children.asSequence().flatMap { child -> sequenceOf(child) + child.descendants() }

private fun SemanticsNode.ancestors(): Sequence<SemanticsNode> = generateSequence(parent) { it.parent }

private fun SemanticsNode.isAncestorOf(other: SemanticsNode): Boolean = other.ancestors().any { it.id == id }

private fun SemanticsNode.scrolls(): Boolean = ancestors().any {
    SemanticsProperties.VerticalScrollAxisRange in it.config || SemanticsProperties.HorizontalScrollAxisRange in it.config
}

private fun SemanticsNode.textLayouts(): List<TextLayoutResult> {
    val layouts = mutableListOf<TextLayoutResult>()
    config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
    return layouts
}

private fun SemanticsNode.label(): String {
    val text = config.getOrNull(SemanticsProperties.Text)?.joinToString(" ")
    val description = config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString(" ")
    val tag = config.getOrNull(SemanticsProperties.TestTag)
    return "\"" + (text ?: description ?: tag ?: "node $id") + "\""
}

/** The width class from which the readable width applies: Large and wider. */
private val WIDE = 1200.dp

/** The smallest target a finger hits reliably, which Compose gives a smaller control's touches. */
private val MIN_TARGET = 48.dp

/** Rounding slack between pixel and dp layouts, in pixels. */
private const val EDGE = 1f
