package com.example.finalafter.crui2.core.designsystem.component.datadisplay

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import com.example.finalafter.crui2.core.designsystem.component.text.TabularFigures
import com.example.finalafter.crui2.core.designsystem.foundation.content.rememberCurrentLocale
import com.example.finalafter.crui2.core.designsystem.preview.AppComponentPreviews
import com.example.finalafter.crui2.core.designsystem.theme.AppTheme
import java.text.NumberFormat

/**
 * A number that rolls to its new value, as iOS's numeric text does: only the digits that change
 * move, up when the number grows and down when it shrinks. For a cart, likes, a balance. The
 * figures keep one width, so the digits that stay never jog. With motion reduced, the number just
 * changes.
 *
 * The number is one drawing: a change lays out its characters once, and the roll redraws them in
 * place rather than recomposing or re-measuring anything.
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
    val reduceMotion = AppTheme.reduceMotion
    val measurer = rememberTextMeasurer()
    val figures = remember(style, color) {
        style.copy(fontFeatureSettings = TabularFigures, color = if (color.isSpecified) color else style.color)
    }
    val text = format.format(count)
    val textLayout = remember(text, figures, measurer) { measurer.measure(text, figures, maxLines = 1, softWrap = false) }
    val roll = remember { CounterRoll(count, textLayout) }
    LaunchedEffect(textLayout) {
        val rolls = roll.start(textLayout, count, measurer, figures)
        if (!rolls || reduceMotion) roll.progress.snapTo(1f) else roll.progress.animateTo(1f, springs.thumb.unit)
    }

    Spacer(
        modifier = modifier
            .clearAndSetSemantics { contentDescription = text }
            .layout { measurable, _ ->
                val placeable = measurable.measure(Constraints.fixed(textLayout.size.width, textLayout.size.height))
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            }
            .clipToBounds()
            .drawBehind { drawRoll(roll) },
    )
}

/**
 * One roll from the number on screen to the next: the characters that change, laid out once when
 * it starts, and how far it has gone.
 */
private class CounterRoll(private var value: Long, shown: TextLayoutResult) {
    val progress = Animatable(1f)

    /**
     * The number being drawn, which becomes the new one only when its roll starts: until then the
     * old number stays on screen rather than flashing the new one for a frame.
     */
    var shown by mutableStateOf(shown)
        private set
    var rollsUp = true
        private set

    /** Each character of the new number, and whether it changed from the one in its place. */
    var glyphs: Array<TextLayoutResult> = emptyArray()
        private set
    var changed = BooleanArray(0)
        private set

    /** The characters of the old number that leave, and where each one was. */
    var leaving: Array<TextLayoutResult?> = emptyArray()
        private set
    var leavingX = FloatArray(0)
        private set

    /** Prepares the roll to [next]; false when no character changes, and nothing needs to roll. */
    suspend fun start(next: TextLayoutResult, count: Long, measurer: TextMeasurer, style: TextStyle): Boolean {
        val previous = shown
        shown = next
        rollsUp = count >= value
        value = count
        val new = next.layoutInput.text.text
        val old = previous.layoutInput.text.text
        if (new == old) return false
        // Numbers grow at the front, so the two line up from their last character.
        val shift = new.length - old.length
        glyphs = Array(new.length) { i -> measurer.measure(new[i].toString(), style, maxLines = 1) }
        changed = BooleanArray(new.length) { i -> old.getOrNull(i - shift) != new[i] }
        leaving = Array(old.length) { j ->
            val i = j + shift
            if (i in new.indices && new[i] == old[j]) null else measurer.measure(old[j].toString(), style, maxLines = 1)
        }
        leavingX = FloatArray(old.length) { j ->
            val i = j + shift
            if (i in new.indices) {
                next.getHorizontalPosition(i, usePrimaryDirection = true)
            } else {
                // In front of where the new number starts: one figure's width per position.
                previous.getHorizontalPosition(j, usePrimaryDirection = true) - previous.size.width + next.size.width
            }
        }
        progress.snapTo(0f)
        return true
    }
}

/**
 * A position whose character did not change is drawn still; one that did has its old character
 * leave and its new one arrive, each travelling the line's height. At rest, the whole number is
 * one draw.
 */
private fun DrawScope.drawRoll(roll: CounterRoll) {
    val layout = roll.shown
    val p = roll.progress.value
    if (p >= 1f) {
        drawText(layout)
        return
    }
    val height = size.height
    val direction = if (roll.rollsUp) -1f else 1f
    val glyphs = roll.glyphs
    for (i in glyphs.indices) {
        val x = layout.getHorizontalPosition(i, usePrimaryDirection = true)
        if (roll.changed[i]) {
            drawText(glyphs[i], topLeft = Offset(x, -direction * (1f - p) * height), alpha = p.coerceIn(0f, 1f))
        } else {
            drawText(glyphs[i], topLeft = Offset(x, 0f))
        }
    }
    val leaving = roll.leaving
    for (j in leaving.indices) {
        val glyph = leaving[j] ?: continue
        drawText(glyph, topLeft = Offset(roll.leavingX[j], direction * p * height), alpha = (1f - p).coerceIn(0f, 1f))
    }
}

@AppComponentPreviews
@Composable
private fun AppAnimatedCounterPreview() {
    Column(
        modifier = Modifier.background(AppTheme.colors.background).padding(AppTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        AppAnimatedCounter(count = 0)
        AppAnimatedCounter(count = 1_204, style = AppTheme.typography.largeTitle)
        AppAnimatedCounter(count = 9_876_543_210, color = AppTheme.colors.accentContent)
    }
}
