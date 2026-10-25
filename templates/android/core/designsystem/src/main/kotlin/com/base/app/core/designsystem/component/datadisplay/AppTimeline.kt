package com.base.app.core.designsystem.component.datadisplay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.preview.AppComponentPreviews
import com.base.app.core.designsystem.theme.AppTheme

/** One point on an [AppTimeline]. */
@Immutable
public data class TimelineStep(
    val title: String,
    val description: String? = null,
    val timestamp: String? = null,
)

/**
 * A vertical trail of steps: an order's status, a history. Each connector is drawn behind its own
 * row and runs to the row's bottom, so it meets the next node whatever the step's text wraps to.
 * Past the current step the trail is dashed: it is there, and still to come.
 */
@Composable
public fun AppTimeline(
    steps: List<TimelineStep>,
    currentIndex: Int,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    Column(modifier = modifier.fillMaxWidth()) {
        steps.forEachIndexed { index, step ->
            val done = index < currentIndex
            val active = index == currentIndex
            val node = when {
                done -> colors.success.base
                active -> colors.accent
                else -> colors.outline
            }
            val last = index == steps.lastIndex
            val connector = if (done) colors.success.base else colors.outline
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawWithCache {
                        val column = AppTimelineDefaults.NodeColumn.toPx()
                        val x = if (layoutDirection == LayoutDirection.Ltr) column / 2f else size.width - column / 2f
                        val top = (if (active) AppTimelineDefaults.NodeActive else AppTimelineDefaults.NodeSize).toPx() + AppTimelineDefaults.NodeGap.toPx()
                        val stroke = AppTimelineDefaults.ConnectorWidth.toPx()
                        val dashes = if (done) null else PathEffect.dashPathEffect(floatArrayOf(stroke * 2f, stroke * 2f))
                        onDrawBehind {
                            if (!last) drawLine(connector, Offset(x, top), Offset(x, size.height), strokeWidth = stroke, pathEffect = dashes)
                        }
                    },
            ) {
                Box(modifier = Modifier.width(AppTimelineDefaults.NodeColumn), contentAlignment = Alignment.TopCenter) {
                    Box(
                        modifier = Modifier.size(if (active) AppTimelineDefaults.NodeActive else AppTimelineDefaults.NodeSize).background(node, AppTheme.shapes.pill),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (done) AppIcon(AppIcons.Check, contentDescription = null, tint = colors.success.on, size = AppTimelineDefaults.CheckSize)
                    }
                }
                Column(
                    modifier = Modifier.weight(1f).padding(start = AppTheme.spacing.md, bottom = AppTheme.spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(AppTimelineDefaults.TextSpacing),
                ) {
                    AppText(
                        text = step.title,
                        style = if (active) AppTheme.typography.body.copy(fontWeight = FontWeight.SemiBold) else AppTheme.typography.body,
                        color = if (index <= currentIndex) colors.contentPrimary else colors.contentSecondary,
                    )
                    step.description?.let {
                        AppText(text = it, style = AppTheme.typography.subheadline, color = colors.contentSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    step.timestamp?.let {
                        AppText(text = it, style = AppTheme.typography.caption1, color = colors.contentSecondary)
                    }
                }
            }
        }
    }
}

/** The timeline's nodes and connectors. */
public object AppTimelineDefaults {
    public val CheckSize: Dp = 10.dp
    public val TextSpacing: Dp = 2.dp
    public val NodeColumn: Dp = 24.dp
    public val NodeSize: Dp = 12.dp
    public val NodeActive: Dp = 16.dp
    public val NodeGap: Dp = 4.dp
    public val ConnectorWidth: Dp = 2.dp
}

@AppComponentPreviews
@Composable
private fun AppTimelinePreview() {
    Column(
        modifier = Modifier.background(AppTheme.colors.background).padding(AppTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        AppTimeline(
            steps = listOf(
                TimelineStep(title = "Ordered", timestamp = "Mon 09:12"),
                TimelineStep(title = "Shipped", description = "Handed to the courier", timestamp = "Tue 16:40"),
                TimelineStep(title = "Out for delivery"),
                TimelineStep(title = "Delivered to the safe place you chose at checkout, behind the side gate"),
            ),
            currentIndex = 1,
        )
    }
}
