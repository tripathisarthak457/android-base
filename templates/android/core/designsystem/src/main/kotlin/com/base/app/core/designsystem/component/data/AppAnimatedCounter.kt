package com.base.app.core.designsystem.component.data

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import com.base.app.core.designsystem.component.text.AppFiguresText
import com.base.app.core.designsystem.foundation.rememberCurrentLocale
import com.base.app.core.designsystem.theme.AppTheme
import java.text.NumberFormat

/**
 * A number that rolls to its new value, up when it grows and down when it shrinks: a cart, likes,
 * a balance. The whole number moves as one, so a change costs one layout, not one per digit, and its
 * figures keep one width. With motion reduced, the number just changes.
 */
@Composable
fun AppAnimatedCounter(
    count: Long,
    modifier: Modifier = Modifier,
    style: TextStyle = AppTheme.typography.title3,
    color: Color = AppTheme.colors.contentPrimary,
) {
    val locale = rememberCurrentLocale()
    val format = remember(locale) { NumberFormat.getIntegerInstance(locale) }
    val springs = AppTheme.springs
    val described = modifier.clearAndSetSemantics { contentDescription = format.format(count) }
    if (AppTheme.reduceMotion) {
        AppFiguresText(text = format.format(count), modifier = described, style = style, color = color)
        return
    }
    AnimatedContent(
        targetState = count,
        modifier = described,
        transitionSpec = {
            val up = targetState >= initialState
            (slideInVertically(springs.selection.offset) { if (up) it else -it } + fadeIn(springs.fadeIn())) togetherWith
                (slideOutVertically(springs.selection.offset) { if (up) -it else it } + fadeOut(springs.fadeOut())) using
                SizeTransform(clip = true)
        },
        label = "counter",
    ) { value ->
        AppFiguresText(text = format.format(value), style = style, color = color)
    }
}
