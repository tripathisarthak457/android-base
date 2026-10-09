package com.base.app.core.designsystem.component.text

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import com.base.app.core.designsystem.foundation.LocalContentColor
import com.base.app.core.designsystem.foundation.LocalTextStyle
import com.base.app.core.designsystem.theme.AppTheme

/**
 * All text in the app. Built on `BasicText`, which is foundation rather than Material and comes
 * with no colour, no style and no notion of a theme. [autoSize] shrinks the text to fit its box.
 */
@Composable
fun AppText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
    textAlign: TextAlign? = null,
    textDecoration: TextDecoration? = null,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    autoSize: TextAutoSize? = null,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.resolve(color, textAlign, textDecoration),
        onTextLayout = onTextLayout,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        autoSize = autoSize,
    )
}

@Composable
fun AppText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
    textAlign: TextAlign? = null,
    textDecoration: TextDecoration? = null,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    autoSize: TextAutoSize? = null,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.resolve(color, textAlign, textDecoration),
        onTextLayout = onTextLayout,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        autoSize = autoSize,
    )
}

/**
 * Text whose colour animates: [color] is read while drawing, so a colour change (a selected tab, a
 * pressed label) redraws the text and never recomposes or re-measures it.
 */
@Composable
fun AppText(
    text: String,
    color: ColorProducer,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    autoSize: TextAutoSize? = null,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = if (textAlign == null) style else style.copy(textAlign = textAlign),
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        color = color,
        autoSize = autoSize,
    )
}

@Composable
private fun TextStyle.resolve(
    color: Color,
    textAlign: TextAlign?,
    textDecoration: TextDecoration?,
): TextStyle {
    val resolvedColor = when {
        color != Color.Unspecified -> color
        this.color != Color.Unspecified -> this.color
        else -> LocalContentColor.current
    }
    if (resolvedColor == this.color && textAlign == null && textDecoration == null) return this
    return copy(
        color = resolvedColor,
        textAlign = textAlign ?: this.textAlign,
        textDecoration = textDecoration ?: this.textDecoration,
    )
}

/**
 * Numbers a person reads aloud, compares digit by digit, or types back: order references, codes,
 * timestamps. Set in the mono face.
 */
@Composable
fun AppMonoText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = AppTheme.typography.mono,
    textAlign: TextAlign? = null,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Ellipsis,
) {
    AppText(
        text = text,
        modifier = modifier,
        color = color,
        style = style,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = overflow,
    )
}

/**
 * A line that mixes words and figures ("12 items · 3 left · 1,240.50"), with every digit the same
 * width, as iOS's monospaced digits are: amounts in a column line up and a changing number does not
 * shift the words around it. The words keep the text face, so the line does not read like code.
 */
@Composable
fun AppFiguresText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    AppText(
        text = text,
        modifier = modifier,
        color = color,
        style = style.copy(fontFeatureSettings = TABULAR_FIGURES),
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = overflow,
    )
}

internal const val TABULAR_FIGURES = "tnum"
