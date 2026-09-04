package com.base.app.core.designsystem.component.feedback

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.foundation.LocalContentColor
import com.base.app.core.designsystem.theme.AppTheme

/**
 * A spinner: a track ring with a single arc travelling around it.
 *
 * Constant angular velocity and a fixed sweep, rather than the accelerating-and-decelerating
 * double animation Material uses. A steady rotation reads as "working" without competing for
 * attention, which matters when one of these is sitting inside a button the user is waiting on.
 *
 * The track is not decoration: without it, a lone arc on a busy surface is hard to locate and the
 * component has no stable visual footprint as it spins.
 */
@Composable
fun AppCircularProgress(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    strokeWidth: Dp = 2.dp,
    color: Color = LocalContentColor.current,
    trackAlpha: Float = 0.20f,
) {
    val transition = rememberInfiniteTransition(label = "spinner")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(ROTATION_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "angle",
    )

    Canvas(modifier = modifier.size(size)) {
        val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        val inset = strokeWidth.toPx() / 2f
        val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
        val topLeft = Offset(inset, inset)

        drawArc(
            color = color.copy(alpha = trackAlpha),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke,
        )
        drawArc(
            color = color,
            startAngle = angle - 90f,
            sweepAngle = SWEEP_DEGREES,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke,
        )
    }
}

/** The same ring, showing a known fraction. [progress] is coerced, so a caller cannot overdraw. */
@Composable
fun AppCircularProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    strokeWidth: Dp = 2.dp,
    color: Color = AppTheme.colors.accent,
    trackAlpha: Float = 0.20f,
) {
    val target = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(AppTheme.motion.medium),
        label = "progress",
    )

    Canvas(
        modifier = modifier
            .size(size)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(target, 0f..1f) },
    ) {
        val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        val inset = strokeWidth.toPx() / 2f
        val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
        val topLeft = Offset(inset, inset)

        drawArc(
            color = color.copy(alpha = trackAlpha),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke,
        )
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360f * animated,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke,
        )
    }
}

/**
 * A determinate bar. Fully rounded at both ends, including at very low progress — a fill that
 * starts as a square sliver and becomes rounded once it is wide enough looks like a rendering
 * bug, so the fill is never drawn narrower than its own height.
 */
@Composable
fun AppLinearProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
    color: Color = AppTheme.colors.accent,
    trackColor: Color = AppTheme.colors.surfaceVariant,
) {
    val target = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(AppTheme.motion.medium, easing = AppTheme.motion.standard),
        label = "linearProgress",
    )

    Canvas(
        modifier = modifier
            .height(height)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(target, 0f..1f) },
    ) {
        val radius = CornerRadius(size.height / 2f)
        drawRoundRect(color = trackColor, size = size, cornerRadius = radius)
        if (animated > 0f) {
            drawRoundRect(
                color = color,
                size = Size(
                    width = (size.width * animated).coerceAtLeast(size.height),
                    height = size.height,
                ),
                cornerRadius = radius,
            )
        }
    }
}

/** An indeterminate bar: a fixed-width segment sweeping the track. */
@Composable
fun AppLinearProgress(
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
    color: Color = AppTheme.colors.accent,
    trackColor: Color = AppTheme.colors.surfaceVariant,
) {
    val transition = rememberInfiniteTransition(label = "linearIndeterminate")
    val head by transition.animateFloat(
        initialValue = -SEGMENT_FRACTION,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(SWEEP_MILLIS, easing = AppTheme.motion.standard),
            repeatMode = RepeatMode.Restart,
        ),
        label = "head",
    )

    Canvas(modifier = modifier.height(height)) {
        val radius = CornerRadius(size.height / 2f)
        drawRoundRect(color = trackColor, size = size, cornerRadius = radius)

        val start = (head * size.width).coerceAtLeast(0f)
        val end = ((head + SEGMENT_FRACTION) * size.width).coerceAtMost(size.width)
        if (end > start) {
            drawRoundRect(
                color = color,
                topLeft = Offset(start, 0f),
                size = Size((end - start).coerceAtLeast(size.height), size.height),
                cornerRadius = radius,
            )
        }
    }
}

private const val ROTATION_MILLIS = 900
private const val SWEEP_DEGREES = 110f
private const val SWEEP_MILLIS = 1200
private const val SEGMENT_FRACTION = 0.35f
