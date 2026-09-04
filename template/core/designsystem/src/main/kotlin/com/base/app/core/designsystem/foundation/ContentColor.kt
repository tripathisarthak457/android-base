package com.base.app.core.designsystem.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

/**
 * The colour text and icons take when they are not told otherwise, and the style text inherits.
 *
 * These exist so that a container can set the contrast for everything inside it *once*. A button
 * with an accent background provides its `onAccent` colour, and the label and icon inside it come
 * out right without either of them being passed a colour. Without this, every component that can
 * appear on a coloured surface needs a `contentColor` parameter threaded through it, and the one
 * place someone forgets renders black text on a dark button.
 *
 * `compositionLocalOf` rather than `staticCompositionLocalOf`: these change during composition —
 * that is the entire point — and a static local would not invalidate the readers when they did.
 */
val LocalContentColor = compositionLocalOf { Color.Black }

val LocalTextStyle = compositionLocalOf { TextStyle.Default }

/** Sets the content colour, and optionally the text style, for a subtree. */
@Composable
fun ProvideContentColor(
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
