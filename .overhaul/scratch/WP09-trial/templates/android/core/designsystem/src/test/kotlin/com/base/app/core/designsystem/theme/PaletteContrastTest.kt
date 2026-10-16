package com.base.app.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test

/** Every text-on-background pair in both palettes, against WCAG AA. */
class PaletteContrastTest {

    @Test
    fun `the light palette meets AA everywhere text sits on a background`() {
        LightColors.assertReadable()
    }

    @Test
    fun `the dark palette meets AA everywhere text sits on a background`() {
        DarkColors.assertReadable()
    }

    @Test
    fun `every design style's surfaces meet AA in both themes`() {
        AppDesignStyle.entries.forEach { design ->
            listOf(LightColors, DarkColors).forEach { base ->
                base.withSurfaces(design.style().surfaces).assertReadable()
            }
        }
    }

    private fun AppColors.assertReadable() {
        val theme = if (isLight) "light" else "dark"

        assertContrast("contentPrimary on background", contentPrimary, background)
        assertContrast("contentPrimary on surface", contentPrimary, surface)
        assertContrast("contentSecondary on surface", contentSecondary, surface)
        assertContrast("contentTertiary on surface", contentTertiary, surface)

        assertContrast("onAccent on accent", onAccent, accent)
        assertContrast("onSecondary on secondary", onSecondary, secondary)
        assertContrast("onTertiary on tertiary", onTertiary, tertiary)

        listOf(
            "success" to success,
            "warning" to warning,
            "danger" to danger,
            "info" to info,
            "neutral" to neutral,
        ).forEach { (name, status) ->
            assertContrast("$name content on its subtle fill ($theme)", status.content, status.subtle)
        }
    }

    private fun assertContrast(what: String, foreground: Color, background: Color) {
        val ratio = contrastRatio(foreground, background)
        assertTrue(
            "$what is ${"%.2f".format(ratio)}:1, below the $MINIMUM_RATIO:1 that AA asks for. " +
                "Darken the foreground or lighten the fill — do not lower this threshold.",
            ratio >= MINIMUM_RATIO,
        )
    }

    private fun contrastRatio(a: Color, b: Color): Double {
        val first = relativeLuminance(a)
        val second = relativeLuminance(b)
        return (maxOf(first, second) + OFFSET) / (minOf(first, second) + OFFSET)
    }

    /** WCAG's own definition, which is not the same as perceptual lightness. */
    private fun relativeLuminance(color: Color): Double {
        fun channel(value: Float): Double {
            val v = value.toDouble()
            return if (v <= LINEAR_LIMIT) v / LINEAR_DIVISOR else ((v + 0.055) / 1.055).pow(2.4)
        }
        return RED_WEIGHT * channel(color.red) +
            GREEN_WEIGHT * channel(color.green) +
            BLUE_WEIGHT * channel(color.blue)
    }

    private fun Double.pow(exponent: Double): Double = Math.pow(this, exponent)

    private companion object {
        const val MINIMUM_RATIO = 4.5
        const val OFFSET = 0.05
        const val LINEAR_LIMIT = 0.03928
        const val LINEAR_DIVISOR = 12.92
        const val RED_WEIGHT = 0.2126
        const val GREEN_WEIGHT = 0.7152
        const val BLUE_WEIGHT = 0.0722
    }
}
