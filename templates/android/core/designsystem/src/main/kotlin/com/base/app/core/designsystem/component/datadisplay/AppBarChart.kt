package com.base.app.core.designsystem.component.datadisplay

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.component.text.AppMonoText
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.preview.AppComponentPreviews
import com.base.app.core.designsystem.theme.AppTheme

/** A horizontal bar chart, one row per value. */
@Composable
public fun AppBarChart(
    entries: List<Pair<String, Float>>,
    modifier: Modifier = Modifier,
    barColor: Color = AppTheme.colors.accent,
    trackColor: Color = AppTheme.colors.fill,
    barHeight: Dp = AppBarChartDefaults.BarHeight,
    valueLabel: (Float) -> String = { it.toInt().toString() },
) {
    if (entries.isEmpty()) return
    val maximum = entries.maxOf { it.second }.takeIf { it > 0f } ?: 1f

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        entries.forEach { (label, value) ->
            val fraction by animateFloatAsState(
                targetValue = value / maximum,
                animationSpec = AppTheme.springs.layout.unit,
                label = "bar",
            )

            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    AppText(
                        text = label,
                        style = AppTheme.typography.subheadline,
                        color = AppTheme.colors.contentSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    AppMonoText(
                        text = valueLabel(value),
                        color = AppTheme.colors.contentTertiary,
                    )
                }
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(barHeight)
                        .clearAndSetSemantics {},
                ) {
                    val radius = CornerRadius(size.height / 2f)
                    drawRoundRect(color = trackColor, size = size, cornerRadius = radius)
                    if (fraction > 0f) {
                        drawRoundRect(
                            color = barColor,
                            size = Size(
                                // Never narrower than the bar is tall, so a small value still
                                // renders as a rounded pill rather than a clipped sliver.
                                width = (size.width * fraction).coerceAtLeast(size.height),
                                height = size.height,
                            ),
                            cornerRadius = radius,
                        )
                    }
                }
            }
        }
    }
}

/** The bar chart's bars. */
public object AppBarChartDefaults {
    public val BarHeight: Dp = 10.dp
}

@AppComponentPreviews
@Composable
private fun AppBarChartPreview() {
    Column(
        modifier = Modifier.background(AppTheme.colors.background).padding(AppTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        AppBarChart(entries = listOf("Mon" to 4f, "Tue" to 9f, "Wed" to 0f, "Thu" to 12f, "Fri" to 7f))
        AppBarChart(
            entries = listOf("Organic search" to 1_840f, "Newsletters and referral partners" to 620f),
            barColor = AppTheme.colors.secondary,
        )
    }
}
