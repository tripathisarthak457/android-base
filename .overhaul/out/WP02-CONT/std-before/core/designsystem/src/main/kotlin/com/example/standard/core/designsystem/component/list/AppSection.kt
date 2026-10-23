package com.example.standard.core.designsystem.component.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.isFinite
import com.example.standard.core.designsystem.component.button.AppLinkButton
import com.example.standard.core.designsystem.component.text.AppText
import com.example.standard.core.designsystem.foundation.adaptive.centeredMaxWidth
import com.example.standard.core.designsystem.theme.AppTheme

/**
 * An inset-grouped section, the list shape iOS uses most: a header, rows sharing one rounded group
 * on the grouped surface with a hairline between them, and a footer. Put it on
 * `AppTheme.colors.backgroundGrouped`.
 *
 * The hairlines start where each row's text starts, which an [AppRow] reports. For a long list in
 * a `LazyColumn`, give each row `shape.groupItem(first, last)` and `Modifier.hairline` instead.
 */
@Composable
fun AppSection(
    modifier: Modifier = Modifier,
    header: String? = null,
    footer: String? = null,
    content: @Composable () -> Unit,
) {
    val spacing = AppTheme.spacing
    Column(modifier = modifier.readableWidth(AppTheme.layout.readableMaxWidth).padding(horizontal = spacing.gutter)) {
        header?.let {
            AppSectionHeader(title = it, modifier = Modifier.padding(horizontal = spacing.lg))
        }
        SeparatedColumn(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppTheme.shapes.md)
                .background(AppTheme.colors.surfaceGrouped),
            content = content,
        )
        footer?.let {
            AppText(
                text = it,
                modifier = Modifier.padding(start = spacing.lg, end = spacing.lg, top = spacing.sm),
                style = AppTheme.typography.footnote,
                color = AppTheme.colors.contentSecondary,
            )
        }
    }
}

/**
 * The small heading over a group of rows, with an optional trailing action. Marked as a heading,
 * so a screen reader can jump between sections.
 */
@Composable
fun AppSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val label = if (AppTheme.style.uppercaseLabels) title.uppercase() else title
    Row(
        modifier = modifier.readableWidth(AppTheme.layout.readableMaxWidth).padding(top = AppTheme.spacing.lg, bottom = AppTheme.spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // On one baseline: the action's 48dp touch target would otherwise lift its text above the title's.
        AppText(
            text = label,
            modifier = Modifier.weight(1f).alignByBaseline().semantics { heading() },
            style = AppTheme.typography.footnote,
            color = AppTheme.colors.contentSecondary,
        )
        if (actionLabel != null && onAction != null) {
            AppLinkButton(
                text = actionLabel,
                onClick = onAction,
                modifier = Modifier.alignByBaseline(),
                style = AppTheme.typography.footnote,
            )
        }
    }
}

/**
 * Where a row's text starts, in pixels from its leading edge, for the hairline above the next row.
 * The row writes it while it is measured, which is before the section reads it to draw.
 */
internal class SeparatorInset : ParentDataModifier {
    var start: Float = Float.NaN

    override fun Density.modifyParentData(parentData: Any?): Any = this@SeparatorInset
}

/** Children stacked top to bottom, with a hairline between each pair drawn by the column itself. */
@Composable
private fun SeparatedColumn(modifier: Modifier, content: @Composable () -> Unit) {
    val lines = remember { SeparatorLines() }
    val color = AppTheme.colors.separator
    val defaultInset = AppTheme.spacing.lg
    Layout(
        content = content,
        modifier = modifier.drawWithContent {
            drawContent()
            val fallback = defaultInset.toPx()
            for (i in 0 until lines.count) {
                val y = lines.y[i] - 0.5f
                val inset = lines.insets[i]?.start?.takeUnless { it.isNaN() } ?: fallback
                val (start, end) = if (layoutDirection == LayoutDirection.Ltr) inset to size.width else 0f to size.width - inset
                drawLine(color, Offset(start, y), Offset(end, y), strokeWidth = 1f)
            }
        },
    ) { measurables: List<Measurable>, constraints: Constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minHeight = 0)) }
        val height = placeables.sumOf { it.height }
        lines.reset(placeables.size - 1)
        var y = 0
        placeables.forEachIndexed { index, placeable ->
            if (index > 0) lines.add(y.toFloat(), measurables[index].parentData as? SeparatorInset)
            y += placeable.height
        }
        layout(constraints.maxWidth, height) {
            var top = 0
            placeables.forEach {
                it.placeRelative(0, top)
                top += it.height
            }
        }
    }
}

/** Written while the column is laid out and read while it draws; a relayout always redraws. */
private class SeparatorLines {
    var y = FloatArray(0)
    var insets = arrayOfNulls<SeparatorInset>(0)
    var count = 0

    fun reset(capacity: Int) {
        if (y.size < capacity) {
            y = FloatArray(capacity)
            insets = arrayOfNulls(capacity)
        }
        count = 0
    }

    fun add(at: Float, inset: SeparatorInset?) {
        y[count] = at
        insets[count] = inset
        count++
    }
}

/** Full width up to the pane's readable width, then centred: a section on a tablet reads as a column. */
private fun Modifier.readableWidth(readable: Dp): Modifier =
    if (readable.isFinite) centeredMaxWidth(readable) else fillMaxWidth()
