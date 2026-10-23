package com.base.app.core.designsystem.component.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.theme.AppTheme

/**
 * An inset-grouped section, as iOS settings draw one: an optional [header] above a rounded group
 * of rows, and an optional [footer] below it explaining them. Put [AppRow]s in [content]; the
 * section draws the hairlines between them, inset to start under their text.
 */
@Composable
fun AppSection(
    modifier: Modifier = Modifier,
    header: String? = null,
    footer: String? = null,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (header != null) {
            AppText(
                text = header,
                modifier = Modifier
                    .padding(horizontal = AppTheme.spacing.lg, vertical = AppTheme.spacing.sm)
                    .semantics { heading() },
                style = AppTheme.typography.footnote,
                color = AppTheme.colors.contentSecondary,
            )
        }
        SeparatedColumn(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppTheme.style.sectionShape)
                .background(AppTheme.colors.surfaceGrouped),
            content = content,
        )
        if (footer != null) {
            AppText(
                text = footer,
                modifier = Modifier.padding(horizontal = AppTheme.spacing.lg, vertical = AppTheme.spacing.sm),
                style = AppTheme.typography.footnote,
                color = AppTheme.colors.contentSecondary,
            )
        }
    }
}

/** A column that draws a hairline between consecutive children and none after the last. */
@Composable
private fun SeparatedColumn(modifier: Modifier, content: @Composable () -> Unit) {
    val color = AppTheme.colors.separator
    val inset = AppTheme.spacing.lg
    // Written while placing and read while drawing, which always follows placement in a frame.
    val separators = remember { ArrayList<Int>() }
    Layout(
        content = content,
        modifier = modifier.drawWithContent {
            drawContent()
            val ltr = layoutDirection == LayoutDirection.Ltr
            val start = if (ltr) inset.toPx() else 0f
            val end = if (ltr) size.width else size.width - inset.toPx()
            separators.forEach { y ->
                drawLine(color, Offset(start, y - 0.5f), Offset(end, y - 0.5f), strokeWidth = 1f)
            }
        },
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minHeight = 0)) }
        val width = constraints.maxWidth
        layout(width, placeables.sumOf { it.height }) {
            separators.clear()
            var y = 0
            placeables.forEachIndexed { index, placeable ->
                placeable.placeRelative(0, y)
                y += placeable.height
                if (index < placeables.lastIndex) separators += y
            }
        }
    }
}
