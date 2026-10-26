package com.example.cmp.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every pairing the palette promises, in both themes: text at WCAG AA (4.5:1) on every background,
 * text on the brand and status fills, controls that must be seen at 3:1, and a segmented thumb on
 * its track. A colour a component adds to `AppColors` brings its own pairing here.
 */
class PaletteContrastTest {

    private val palettes: List<Pair<String, AppColors>> = listOf(false, true).map { dark ->
        (if (dark) "dark" else "light") to appColors(dark)
    }

    @Test
    fun `text roles read on every background`() = palettes.forEach { (name, colors) ->
        colors.backgrounds.forEach { background ->
            assertAtLeast("$name contentPrimary", colors.contentPrimary, background, AaText)
            assertAtLeast("$name contentSecondary", colors.contentSecondary, background, AaText)
            assertAtLeast("$name contentTertiary", colors.contentTertiary, background, AaText)
        }
    }

    @Test
    fun `the accent as text reads on every background and on its own tint`() = palettes.forEach { (name, colors) ->
        (colors.backgrounds + colors.accentSubtle).forEach { background ->
            assertAtLeast("$name accentContent", colors.accentContent, background, AaText)
        }
    }

    @Test
    fun `text on brand fills reads`() = palettes.forEach { (name, colors) ->
        assertAtLeast("$name onAccent on accent", colors.onAccent, colors.accent, AaText)
        assertAtLeast("$name onSecondary on secondary", colors.onSecondary, colors.secondary, AaText)
        assertAtLeast("$name onTertiary on tertiary", colors.onTertiary, colors.tertiary, AaText)
    }

    @Test
    fun `status colours read on surfaces, on their tint, and under their own text`() = palettes.forEach { (name, colors) ->
        listOf("success" to colors.success, "warning" to colors.warning, "danger" to colors.danger, "info" to colors.info)
            .forEach { (meaning, status) ->
                (colors.backgrounds + status.subtle).forEach { background ->
                    assertAtLeast("$name $meaning content", status.content, background, AaText)
                }
                assertAtLeast("$name $meaning on its base", status.on, status.base, AaText)
            }
    }

    @Test
    fun `controls that must be seen stand out at 3 to 1`() = palettes.forEach { (name, colors) ->
        colors.backgrounds.forEach { background ->
            assertAtLeast("$name outline (checkbox ring, field edge, off switch)", colors.outline, background, AaNonText)
            assertAtLeast("$name accent as a control", colors.accent, background, AaNonText)
        }
    }

    @Test
    fun `a segmented thumb stands apart from its track`() = palettes.forEach { (name, colors) ->
        if (colors.isLight) {
            // iOS's own pairing: the lightest surface on a track a step darker, lifted by its shadow.
            assertTrue("$name thumb is the lighter of the two", colors.segmentThumb.luminance() > colors.fill.luminance())
            assertAtLeast("$name thumb on track", colors.segmentThumb, colors.fill, LightThumbOnTrack)
        } else {
            assertAtLeast("$name thumb on track", colors.segmentThumb, colors.fill, ThumbOnTrack)
        }
        assertAtLeast("$name label on the thumb", colors.contentPrimary, colors.segmentThumb, AaText)
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

    private companion object {
        const val AaText = 4.5f
        const val AaNonText = 3f
        const val ThumbOnTrack = 1.5f
        const val LightThumbOnTrack = 1.15f
    }
}
