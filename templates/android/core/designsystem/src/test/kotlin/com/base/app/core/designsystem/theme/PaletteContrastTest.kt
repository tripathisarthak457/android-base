package com.base.app.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every pairing the components draw, in every design style and both themes: text at WCAG AA (4.5:1)
 * on every background, controls that must be seen at 3:1, and the tab bar's selection.
 */
class PaletteContrastTest {

    private val palettes: List<Pair<String, AppColors>> = AppDesignStyle.entries.flatMap { design ->
        listOf(LightColors, DarkColors).map { base ->
            "${design.name} ${if (base.isLight) "light" else "dark"}" to base.withSurfaces(design.style().surfaces)
        }
    }

    @Test
    fun `text roles read on every background`() = palettes.forEach { (name, colors) ->
        colors.backgrounds.forEach { background ->
            assertAtLeast("$name contentPrimary", colors.contentPrimary, background, AA_TEXT)
            assertAtLeast("$name contentSecondary", colors.contentSecondary, background, AA_TEXT)
            assertAtLeast("$name contentTertiary", colors.contentTertiary, background, AA_TEXT)
        }
    }

    @Test
    fun `the accent as text reads on every background and on its own tint`() = palettes.forEach { (name, colors) ->
        (colors.backgrounds + colors.accentSubtle).forEach { background ->
            assertAtLeast("$name accentContent", colors.accentContent, background, AA_TEXT)
        }
    }

    @Test
    fun `text on brand fills reads`() = palettes.forEach { (name, colors) ->
        assertAtLeast("$name onAccent on accent", colors.onAccent, colors.accent, AA_TEXT)
        assertAtLeast("$name onSecondary on secondary", colors.onSecondary, colors.secondary, AA_TEXT)
        assertAtLeast("$name onTertiary on tertiary", colors.onTertiary, colors.tertiary, AA_TEXT)
    }

    @Test
    fun `status colours read on surfaces, on their tint, and under their own text`() = palettes.forEach { (name, colors) ->
        listOf("success" to colors.success, "warning" to colors.warning, "danger" to colors.danger, "info" to colors.info)
            .forEach { (meaning, status) ->
                (colors.backgrounds + status.subtle).forEach { background ->
                    assertAtLeast("$name $meaning content", status.content, background, AA_TEXT)
                }
                assertAtLeast("$name $meaning on its base", status.on, status.base, AA_TEXT)
            }
    }

    @Test
    fun `controls that must be seen stand out at 3 to 1`() = palettes.forEach { (name, colors) ->
        colors.backgrounds.forEach { background ->
            assertAtLeast("$name outline (checkbox ring, field edge, off switch)", colors.outline, background, AA_NON_TEXT)
            assertAtLeast("$name accent as a control", colors.accent, background, AA_NON_TEXT)
        }
    }

    @Test
    fun `the tab bar shows its selection without relying on a faint pill`() {
        AppDesignStyle.entries.forEach { design ->
            val style = design.style()
            listOf(LightColors, DarkColors).forEach { base ->
                val colors = base.withSurfaces(style.surfaces)
                val name = "${design.name} ${if (colors.isLight) "light" else "dark"} ${style.bar}"
                val bar = barSurface(style.bar, colors)
                when (style.indicator) {
                    BarIndicator.Pill -> {
                        assertAtLeast("$name barIndicator against the bar", colors.barIndicator, bar, AA_NON_TEXT)
                        assertAtLeast("$name onAccent on the indicator", colors.onAccent, colors.barIndicator, AA_TEXT)
                    }
                    BarIndicator.Subtle -> assertAtLeast("$name selected glyph on the bar", colors.accentContent, bar, AA_TEXT)
                }
            }
        }
    }

    private fun barSurface(treatment: BarTreatment, colors: AppColors): Color = when (treatment) {
        BarTreatment.Floating -> colors.surfaceElevated
        BarTreatment.Translucent -> colors.surface.copy(alpha = 0.88f).compositeOver(colors.background)
        BarTreatment.Docked -> colors.surface
    }

    private fun assertAtLeast(what: String, foreground: Color, background: Color, minimum: Float) {
        val ratio = wcagContrast(foreground, background)
        assertTrue(
            "$what is ${"%.2f".format(ratio)}:1, below $minimum:1. Darken the foreground or lighten the fill; " +
                "do not lower this threshold.",
            ratio >= minimum,
        )
    }

    /** WCAG 2's definition, computed here rather than with the palette's own helper. */
    private fun wcagContrast(a: Color, b: Color): Double {
        fun channel(value: Float): Double {
            val v = value.toDouble()
            return if (v <= 0.03928) v / 12.92 else Math.pow((v + 0.055) / 1.055, 2.4)
        }
        fun luminance(c: Color) = 0.2126 * channel(c.red) + 0.7152 * channel(c.green) + 0.0722 * channel(c.blue)
        val first = luminance(a)
        val second = luminance(b)
        return (maxOf(first, second) + 0.05) / (minOf(first, second) + 0.05)
    }
}
