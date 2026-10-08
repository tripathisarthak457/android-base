package com.base.app.core.designsystem.component.text

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import com.base.app.core.designsystem.foundation.rememberCurrentLocale
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.rememberReduceMotion
import java.text.NumberFormat

/**
 * A number that rolls to its new value, each digit sliding up when it grows and down when it
 * shrinks, instead of jumping. For counts that change while someone watches: a cart, likes,
 * a balance.
 *
 * Digits are tabular, so the width holds still while they change. With the system's animations
 * off, the number just changes.
 */
@Composable
fun AppAnimatedCounter(
    count: Long,
    modifier: Modifier = Modifier,
    style: TextStyle = AppTheme.typography.titleLarge,
    color: Color = AppTheme.colors.contentPrimary,
    format: (Long) -> String = rememberGroupedFormat(),
) {
    val text = format(count)
    val tabular = style.copy(fontFeatureSettings = "tnum")
    val described = modifier.clearAndSetSemantics { contentDescription = text }

    if (rememberReduceMotion()) {
        AppText(text = text, modifier = described, style = tabular, color = color)
        return
    }

    // Not state: the direction is read while composing the new value, and remembering what was
    // shown must not ask for another composition.
    val lastShown = remember { LastShown(count) }
    val rising = count >= lastShown.value
    SideEffect { lastShown.value = count }
    val motion = AppTheme.motion

    Row(modifier = described) {
        // Keyed from the right, so the units digit stays the units digit when a new place appears.
        text.forEachIndexed { index, character ->
            key(text.length - index) {
                AnimatedContent(
                    targetState = character,
                    transitionSpec = {
                        val direction = if (rising) 1 else -1
                        (
                            slideInVertically(tween(motion.medium, easing = motion.enter)) { it * direction } +
                                fadeIn(tween(motion.medium))
                            ) togetherWith (
                            slideOutVertically(tween(motion.medium, easing = motion.exit)) { -it * direction } +
                                fadeOut(tween(motion.quick))
                            ) using SizeTransform(clip = true)
                    },
                    label = "counterDigit",
                ) { digit ->
                    AppText(text = digit.toString(), style = tabular, color = color)
                }
            }
        }
    }
}

@Composable
private fun rememberGroupedFormat(): (Long) -> String {
    val locale = rememberCurrentLocale()
    return remember(locale) {
        val formatter = NumberFormat.getIntegerInstance(locale)
        val format: (Long) -> String = { value -> formatter.format(value) }
        format
    }
}

private class LastShown(var value: Long)
