package com.base.app.core.designsystem.component.feedback

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.theme.AppTheme

/** A placeholder block with a highlight sweeping across it. */
@Composable
fun AppSkeleton(
    modifier: Modifier = Modifier,
    shape: Shape = AppTheme.shapes.xs,
) {
    val base = AppTheme.colors.skeleton
    val highlight = if (AppTheme.colors.isLight) {
        AppTheme.colors.surface
    } else {
        AppTheme.colors.surfaceVariant
    }

    val transition = rememberInfiniteTransition(label = "skeleton")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(SWEEP_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sweep",
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(base)
            .drawWithCache {
                val width = size.width
                // The gradient is three widths long and travels four, so it is fully off-screen at
                // both ends.
                val start = -width + progress * (width * 3f)
                val brush = Brush.linearGradient(
                    colors = listOf(base, highlight, base),
                    start = Offset(start, 0f),
                    end = Offset(start + width, 0f),
                )
                onDrawBehind { drawRect(brush) }
            }
            .clearAndSetSemantics {},
    )
}

/** A single line of placeholder text, at the height of the style it stands in for. */
@Composable
fun AppSkeletonLine(
    modifier: Modifier = Modifier,
    height: Dp = 14.dp,
    widthFraction: Float = 1f,
) {
    AppSkeleton(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .height(height),
        shape = AppTheme.shapes.xs,
    )
}

@Composable
fun AppSkeletonCircle(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    AppSkeleton(modifier = modifier.size(size), shape = AppTheme.shapes.pill)
}

/** A stand-in for a list row: a circle and two lines of different lengths. */
@Composable
fun AppSkeletonListItem(
    modifier: Modifier = Modifier,
    showLeading: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        if (showLeading) AppSkeletonCircle(size = 44.dp)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        ) {
            AppSkeletonLine(height = 14.dp, widthFraction = 0.65f)
            AppSkeletonLine(height = 12.dp, widthFraction = 0.4f)
        }
    }
}

private const val SWEEP_MILLIS = 1400
