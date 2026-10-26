package com.example.cmp.core.designsystem.foundation.content

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

/** The colour text and icons take when they are not told otherwise, and the style text inherits. */
public val LocalContentColor: ProvidableCompositionLocal<Color> = compositionLocalOf { Color.Black }

public val LocalTextStyle: ProvidableCompositionLocal<TextStyle> =
    compositionLocalOf { TextStyle.Default }

/** Sets the content colour, and optionally the text style, for a subtree. */
@Composable
public fun ProvideContentColor(
    color: Color,
    textStyle: TextStyle? = null,
    content: @Composable () -> Unit,
) {
    if (textStyle == null) {
        CompositionLocalProvider(LocalContentColor provides color, content = content)
    } else {
        CompositionLocalProvider(
            LocalContentColor provides color,
            LocalTextStyle provides textStyle,
            content = content,
        )
    }
}
