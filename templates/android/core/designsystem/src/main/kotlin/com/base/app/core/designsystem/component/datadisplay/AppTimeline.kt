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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.theme.AppTheme

/** One point on an [AppTimeline]. */
@Immutable
data class TimelineStep(
    val title: String,
    val description: String? = null,
    val timestamp: String? = null,
)

/**
 * A vertical trail of steps: an order's status, a history. Each connector is drawn behind its own
 * row and runs to the row's bottom, so it meets the next node whatever the step's text wraps to.
 */
@Composable
fun AppTimeline(
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
            val connector = if (done) colors.success.base else colors.separator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        if (last) return@drawBehind
                        val column = NodeColumn.toPx()
                        val x = if (layoutDirection == LayoutDirection.Ltr) column / 2f else size.width - column / 2f
                        val top = (if (active) NodeActive else NodeSize).toPx() + NodeGap.toPx()
                        drawLine(connector, Offset(x, top), Offset(x, size.height), strokeWidth = ConnectorWidth.toPx())
                    },
            ) {
                Box(modifier = Modifier.width(NodeColumn), contentAlignment = Alignment.TopCenter) {
                    Box(
                        modifier = Modifier.size(if (active) NodeActive else NodeSize).background(node, AppTheme.shapes.pill),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (done) AppIcon(AppIcons.Check, contentDescription = null, tint = colors.success.on, size = 10.dp)
                    }
                }
                Column(
                    modifier = Modifier.weight(1f).padding(start = AppTheme.spacing.md, bottom = AppTheme.spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
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

private val NodeColumn = 24.dp
private val NodeSize = 12.dp
private val NodeActive = 16.dp
private val NodeGap = 4.dp
private val ConnectorWidth = 2.dp
