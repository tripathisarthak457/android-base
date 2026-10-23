package com.example.lean.core.designsystem.component.text

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import com.example.lean.core.designsystem.foundation.content.LocalContentColor
import com.example.lean.core.designsystem.foundation.content.LocalTextStyle
import com.example.lean.core.designsystem.foundation.draw.LocalRedacted
import com.example.lean.core.designsystem.foundation.draw.TextBones
import com.example.lean.core.designsystem.foundation.draw.textSkeleton
import com.example.lean.core.designsystem.theme.AppTheme

/**
 * All text in the app. Built on `BasicText`, which is foundation rather than Material and comes
 * with no colour, no style and no notion of a theme. [autoSize] shrinks the text to fit its box.
 *
 * Inside an active `AppRedacted` the text is laid out as usual but drawn as one placeholder bone per
 * line, so a placeholder takes exactly the room the real text will.
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
    val bones = rememberTextBones()
    BasicText(
        text = text,
        modifier = modifier.redactedAs(bones),
        style = style.resolve(color, textAlign, textDecoration),
        onTextLayout = rememberLayoutListener(bones, onTextLayout),
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
    val bones = rememberTextBones()
    BasicText(
        text = text,
        modifier = modifier.redactedAs(bones),
        style = style.resolve(color, textAlign, textDecoration),
        onTextLayout = rememberLayoutListener(bones, onTextLayout),
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
    val bones = rememberTextBones()
    BasicText(
        text = text,
        modifier = modifier.redactedAs(bones),
        style = if (textAlign == null) style else style.copy(textAlign = textAlign),
        onTextLayout = rememberLayoutListener(bones, null),
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        color = color,
        autoSize = autoSize,
    )
}

/** The bones to draw this text as, inside an active `AppRedacted`; null when it draws as text. */
@Composable
private fun rememberTextBones(): TextBones? = if (LocalRedacted.current) remember { TextBones() } else null

private fun Modifier.redactedAs(bones: TextBones?): Modifier = if (bones == null) this else textSkeleton(bones)

/** [onTextLayout], also keeping [bones] in step with each layout of the text they stand for. */
@Composable
private fun rememberLayoutListener(
    bones: TextBones?,
    onTextLayout: ((TextLayoutResult) -> Unit)?,
): ((TextLayoutResult) -> Unit)? {
    if (bones == null) return onTextLayout
    return remember(bones, onTextLayout) {
        { layout ->
            bones.update(layout)
            onTextLayout?.invoke(layout)
        }
    }
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
        style = style.copy(fontFeatureSettings = TabularFigures),
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = overflow,
    )
}

internal const val TabularFigures = "tnum"
