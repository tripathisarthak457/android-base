package com.base.app.core.designsystem.component.text

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
 * All text in the app.
 *
 * Built on `BasicText`, which is foundation rather than Material and comes with no colour, no
 * style and no notion of a theme. Everything Material's `Text` supplied is supplied here instead:
 * the inherited [LocalTextStyle], the inherited [LocalContentColor], and the merge order between
 * them and whatever the caller passed.
 *
 * That merge order is the part worth stating. An explicit [color] wins; failing that, a colour
 * set on the [style] wins; failing that, the surrounding [LocalContentColor]. Which means a label
 * inside a coloured button is correct without being told, and a caller who does want to override
 * still can.
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
    return merge(
        TextStyle(
            color = resolvedColor,
            textAlign = textAlign ?: TextAlign.Unspecified,
            textDecoration = textDecoration,
        ),
    )
}

/**
 * Numbers a person reads aloud, compares digit by digit, or types back: order references,
 * amounts in a column, codes, timestamps.
 *
 * Monospaced so that `0` and `O` cannot be mistaken for one another and so that a column of
 * amounts aligns on the decimal without any layout work.
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
