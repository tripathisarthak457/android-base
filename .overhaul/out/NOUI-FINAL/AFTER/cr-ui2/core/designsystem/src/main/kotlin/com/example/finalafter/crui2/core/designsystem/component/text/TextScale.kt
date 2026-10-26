package com.example.finalafter.crui2.core.designsystem.component.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

/**
 * [this] grown by as much as text in [style] grows at the current font scale, so a glyph set beside
 * that text keeps its proportion to it. It follows the platform's nonlinear scaling of that size.
 */
@Composable
@ReadOnlyComposable
internal fun Dp.scaledLike(style: TextStyle): Dp {
    val size = style.fontSize
    if (!size.isSp || size.value <= 0f) return this
    val rendered = with(LocalDensity.current) { size.toDp() }
    return this * (rendered.value / size.value)
}

/**
 * [this] grown linearly with the font scale, up to [maxScale]. Android 14 and later scale large
 * sizes far less than body text, so a 34 sp title stays 34 sp at 1.5x; a display title that should
 * still grow with the person's setting, as iOS's large title does, uses this instead.
 */
@Composable
@ReadOnlyComposable
internal fun TextStyle.grownWithFontScale(maxScale: Float): TextStyle {
    val density = LocalDensity.current
    val scale = density.fontScale.coerceIn(1f, maxScale)
    fun TextUnit.grown(): TextUnit = if (isSp) with(density) { (value * scale).dp.toSp() } else this
    return copy(fontSize = fontSize.grown(), lineHeight = lineHeight.grown())
}
