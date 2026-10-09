package com.base.app.core.designsystem.component.datadisplay

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.R
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.component.text.TABULAR_FIGURES
import com.base.app.core.designsystem.foundation.draw.HairlineEdge
import com.base.app.core.designsystem.foundation.draw.hairline
import com.base.app.core.designsystem.foundation.interaction.PressFeedback
import com.base.app.core.designsystem.foundation.interaction.pressable
import com.base.app.core.designsystem.theme.AppTheme

/** How a column's values read and line up. Figures keep one width per digit so they align. */
enum class AppColumnType(internal val defaultWidth: Dp, internal val alignEnd: Boolean, internal val figures: Boolean) {
    Text(defaultWidth = 160.dp, alignEnd = false, figures = false),
    Number(defaultWidth = 88.dp, alignEnd = true, figures = true),
    Money(defaultWidth = 124.dp, alignEnd = true, figures = true),
    Date(defaultWidth = 112.dp, alignEnd = false, figures = true),
}

/**
 * One column of an [AppDataTable]. The caller formats every value and [total]; the table knows how
 * to line them up, not what a currency looks like.
 */
@Immutable
class AppTableColumn<T>(
    val label: String,
    val type: AppColumnType = AppColumnType.Text,
    val width: Dp = type.defaultWidth,
    val total: String? = null,
    val value: (T) -> String,
)

/**
 * A report table: a header that stays on top while the rows scroll, columns that scroll sideways
 * together on a phone and spread to fill a tablet, zebra rows, a tap on a row, and a totals row.
 *
 * It scrolls itself, so give it a bounded height rather than putting it in another vertical scroll.
 * What should scroll away above the header goes in [leading]; what is read after the rows in
 * [trailing].
 */
@Composable
fun <T> AppDataTable(
    columns: List<AppTableColumn<T>>,
    rows: List<T>,
    modifier: Modifier = Modifier,
    rowKey: ((T) -> Any)? = null,
    onRowClick: ((T) -> Unit)? = null,
    rowClickLabel: String? = null,
    totalsLabel: String = stringResource(R.string.designsystem_total),
    zebra: Boolean = true,
    listState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(),
    leading: (LazyListScope.() -> Unit)? = null,
    trailing: (LazyListScope.() -> Unit)? = null,
) {
    // One sideways scroll for the header, every row and the totals, so a column never drifts from
    // its heading whichever row the finger lands on.
    val horizontal = rememberScrollState()
    val hasTotals = columns.any { it.total != null }
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val figureStyle = AppTheme.typography.subheadline.copy(fontFeatureSettings = TABULAR_FIGURES)
    val cellPadding = AppTheme.spacing.md * 2
    val fadeWidth = AppTheme.spacing.xxxl
    val colors = AppTheme.colors

    BoxWithConstraints(modifier = modifier) {
        // Keyed on what the columns show rather than the list, which callers build anew on every
        // composition: measuring each figure again would cost a frame on a long report.
        val shape = columns.map { listOf(it.label, it.type, it.width, it.total) }
        val widths = remember(shape, rows, maxWidth, figureStyle, density) {
            val needed = longestFigures(columns, rows).map { figure ->
                figure?.let { with(density) { measurer.measure(it, figureStyle, maxLines = 1, softWrap = false).size.width.toDp() } + cellPadding }
            }
            fittedWidths(widenedToFit(columns.map { it.width }, needed), maxWidth)
        }

        LazyColumn(state = listState, contentPadding = contentPadding, modifier = Modifier.fillMaxSize()) {
            leading?.invoke(this)
            stickyHeader(key = HEADER_KEY, contentType = HEADER_KEY) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.backgroundGrouped)
                        .overflowFade(horizontal, colors.backgroundGrouped, fadeWidth)
                        .horizontalScroll(horizontal)
                        .defaultMinSize(minHeight = AppTheme.sizes.minTouchTarget)
                        .semantics { heading() },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    columns.forEachIndexed { index, column ->
                        Cell(column.label, widths[index], column.type.alignEnd, AppTheme.typography.caption1, colors.contentSecondary, maxLines = 2)
                    }
                }
            }
            itemsIndexed(
                items = rows,
                key = rowKey?.let { key -> { _: Int, row: T -> key(row) } },
                contentType = { _, _ -> ROW_TYPE },
            ) { index, row ->
                val background = if (zebra && index % 2 == 1) colors.backgroundGrouped else colors.surface
                val values = columns.map { it.value(row) }
                // Read as "Date 05-10-2026, Freight 1,943.76": cells alone lose which number is which.
                val spoken = columns.indices.map { stringResource(R.string.designsystem_table_cell, columns[it].label, values[it]) }.joinToString()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(background)
                        .then(
                            if (onRowClick != null) {
                                Modifier.pressable(onClick = { onRowClick(row) }, feedback = PressFeedback.Highlight, onClickLabel = rowClickLabel)
                            } else {
                                Modifier
                            },
                        )
                        .overflowFade(horizontal, background, fadeWidth)
                        .horizontalScroll(horizontal)
                        .defaultMinSize(minHeight = AppTheme.sizes.minTouchTarget)
                        .clearAndSetSemantics { contentDescription = spoken },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    columns.forEachIndexed { i, column ->
                        Cell(
                            values[i],
                            widths[i],
                            column.type.alignEnd,
                            if (column.type.figures) figureStyle else AppTheme.typography.subheadline,
                            colors.contentPrimary,
                            maxLines = 2,
                        )
                    }
                }
            }
            if (hasTotals) {
                item(key = TOTALS_KEY, contentType = TOTALS_KEY) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.surface)
                            .hairline(HairlineEdge.Top)
                            .overflowFade(horizontal, colors.surface, fadeWidth)
                            .horizontalScroll(horizontal)
                            .defaultMinSize(minHeight = AppTheme.sizes.minTouchTarget),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        columns.forEachIndexed { index, column ->
                            Cell(
                                column.total ?: if (index == 0) totalsLabel else "",
                                widths[index],
                                column.type.alignEnd,
                                (if (column.type.figures && column.total != null) figureStyle else AppTheme.typography.subheadline).copy(fontWeight = FontWeight.SemiBold),
                                colors.contentPrimary,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
            trailing?.invoke(this)
        }
    }
}

/**
 * Fades a row into its own background on the side where columns wait out of sight, so a table cut
 * at the window's edge says that it goes on. Read while drawing.
 */
private fun Modifier.overflowFade(scroll: ScrollState, color: Color, width: Dp): Modifier = drawWithContent {
    drawContent()
    val fade = width.toPx().coerceAtMost(size.width / 2)
    if (scroll.canScrollForward) {
        drawRect(
            brush = Brush.horizontalGradient(listOf(Color.Transparent, color), startX = size.width - fade, endX = size.width),
            topLeft = Offset(size.width - fade, 0f),
            size = Size(fade, size.height),
        )
    }
    if (scroll.canScrollBackward) {
        drawRect(brush = Brush.horizontalGradient(listOf(color, Color.Transparent), startX = 0f, endX = fade), size = Size(fade, size.height))
    }
}

@Composable
private fun Cell(text: String, width: Dp, alignEnd: Boolean, style: TextStyle, color: Color, maxLines: Int) {
    AppText(
        text = text,
        modifier = Modifier.width(width).padding(horizontal = AppTheme.spacing.md, vertical = AppTheme.spacing.sm),
        style = style,
        color = color,
        textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

/**
 * Per figure column, the value or total with the most characters, null for a column of words. Its
 * digits are all one width, so a figure's width follows its length and one measurement per column
 * is enough; measuring every cell of a long report cost a frame.
 */
internal fun <T> longestFigures(columns: List<AppTableColumn<T>>, rows: List<T>): List<String?> =
    columns.map { column ->
        if (!column.type.figures) return@map null
        (rows.asSequence().map(column.value) + listOfNotNull(column.total)).maxByOrNull { it.length }
    }

/** Each column at least as wide as its widest figure needs, where it has figures. */
internal fun widenedToFit(widths: List<Dp>, needed: List<Dp?>): List<Dp> =
    widths.mapIndexed { index, width -> needed.getOrNull(index)?.let { maxOf(width, it) } ?: width }

/**
 * The columns at their own widths when they overflow the window, so the table scrolls sideways, or
 * stretched in proportion when they leave room over, so a tablet shows a full-width table.
 */
internal fun fittedWidths(widths: List<Dp>, available: Dp): List<Dp> {
    val total = widths.fold(0.dp) { sum, width -> sum + width }
    if (total <= 0.dp || total >= available || available == Dp.Infinity) return widths
    val scale = available / total
    return widths.map { it * scale }
}

private const val HEADER_KEY = "app-data-table-header"
private const val TOTALS_KEY = "app-data-table-totals"
private const val ROW_TYPE = "app-data-table-row"
