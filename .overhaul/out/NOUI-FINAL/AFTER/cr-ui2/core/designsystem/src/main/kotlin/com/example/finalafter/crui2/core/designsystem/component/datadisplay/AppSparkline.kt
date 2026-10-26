package com.example.finalafter.crui2.core.designsystem.component.datadisplay

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.finalafter.crui2.core.designsystem.preview.AppComponentPreviews
import com.example.finalafter.crui2.core.designsystem.theme.AppTheme

/** A tiny chart, sized to sit inside a row or a stat tile. */
@Composable
fun AppSparkline(
    values: List<Float>,
    modifier: Modifier = Modifier,
    color: Color = AppTheme.colors.accent,
    strokeWidth: Dp = AppSparklineDefaults.StrokeWidth,
    height: Dp = AppSparklineDefaults.Height,
    fill: Boolean = true,
) {
    if (values.size < 2) return

    val progress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = AppTheme.springs.layout.unit,
        label = "sparkline",
    )

    // Height is a parameter because a Canvas measures to zero unless its size is fixed.
    Canvas(
        modifier = modifier
            .height(height)
            .clearAndSetSemantics {},
    ) {
        val minimum = values.min()
        val maximum = values.max()
        val span = (maximum - minimum).takeIf { it > 0f } ?: 1f
        val stepX = size.width / (values.size - 1)

        fun pointAt(index: Int) = Offset(
            x = stepX * index,
            // Inset by the stroke so the extremes are not clipped in half by the canvas edge.
            y = size.height - ((values[index] - minimum) / span) *
                (size.height - strokeWidth.toPx()) - strokeWidth.toPx() / 2f,
        )

        val visiblePoints = (values.size * progress).toInt().coerceAtLeast(2)
        val line = Path().apply {
            moveTo(pointAt(0).x, pointAt(0).y)
            for (index in 1 until visiblePoints) {
                lineTo(pointAt(index).x, pointAt(index).y)
            }
        }

        if (fill) {
            val area = Path().apply {
                addPath(line)
                lineTo(pointAt(visiblePoints - 1).x, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(
                path = area,
                brush = Brush.verticalGradient(
                    listOf(color.copy(alpha = FillAlpha), Color.Transparent),
                ),
            )
        }

        drawPath(
            path = line,
            color = color,
            style = Stroke(
                width = strokeWidth.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}

private const val FillAlpha = 0.22f

/** The sparkline's line and height. */
object AppSparklineDefaults {
    val StrokeWidth = 2.dp
    val Height = 48.dp
}

@AppComponentPreviews
@Composable
private fun AppSparklinePreview() {
    Column(
        modifier = Modifier.background(AppTheme.colors.background).padding(AppTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        AppSparkline(values = listOf(3f, 5f, 4f, 8f, 6f, 9f, 12f), modifier = Modifier.fillMaxWidth())
        AppSparkline(values = listOf(9f, 7f, 8f, 4f, 2f), modifier = Modifier.fillMaxWidth(), color = AppTheme.colors.danger.base, fill = false)
        AppSparkline(values = listOf(5f), modifier = Modifier.fillMaxWidth())
    }
}
