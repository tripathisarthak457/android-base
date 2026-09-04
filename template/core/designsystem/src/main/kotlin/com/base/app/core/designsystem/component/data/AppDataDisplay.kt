package com.base.app.core.designsystem.component.data

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.component.text.AppMonoText
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.AppSurface
import com.base.app.core.designsystem.foundation.appClickable
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.StatusColors

/**
 * A label-and-value row, for a detail screen.
 *
 * The value is monospaced when [mono] is set — order numbers, references, amounts, anything the
 * user might read aloud or compare character by character. That is not decoration: `0` and `O`
 * are genuinely ambiguous in a proportional face, and a column of amounts only aligns on the
 * decimal in a monospaced one.
 *
 * The value wraps and the label does not. A truncated label leaves the reader unable to tell what
 * they are looking at; a truncated value loses the thing they came for.
 */
@Composable
fun AppDetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    mono: Boolean = false,
    valueColor: Color = AppTheme.colors.contentPrimary,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.appClickable(onClick = onClick, minTouchTarget = 0.dp)
                } else {
                    Modifier
                },
            )
            .padding(vertical = AppTheme.spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        verticalAlignment = Alignment.Top,
    ) {
        icon?.let {
            AppIcon(it, contentDescription = null, tint = AppTheme.colors.contentTertiary, size = 16.dp)
        }
        AppText(
            text = label,
            modifier = Modifier.weight(LABEL_WEIGHT),
            style = AppTheme.typography.bodySmall,
            color = AppTheme.colors.contentTertiary,
        )
        if (mono) {
            AppMonoText(
                text = value,
                modifier = Modifier.weight(VALUE_WEIGHT),
                color = valueColor,
                textAlign = TextAlign.End,
                maxLines = 2,
            )
        } else {
            AppText(
                text = value,
                modifier = Modifier.weight(VALUE_WEIGHT),
                style = AppTheme.typography.bodyMedium,
                color = valueColor,
                textAlign = TextAlign.End,
            )
        }
        if (onClick != null) {
            AppIcon(
                AppIcons.ChevronRight,
                contentDescription = null,
                tint = AppTheme.colors.contentTertiary,
                size = 16.dp,
            )
        }
    }
}

/**
 * A single headline number with its label, and optionally which way it moved.
 *
 * The delta carries an arrow as well as a colour, because "up 12%" rendered only in green is
 * invisible to a red-green colour-blind reader — and on a dashboard, direction is the entire
 * message.
 */
@Composable
fun AppStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    delta: String? = null,
    deltaIsPositive: Boolean = true,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = AppTheme.colors
    val deltaColors: StatusColors = if (deltaIsPositive) colors.success else colors.danger

    AppSurface(
        modifier = modifier.then(
            if (onClick != null) Modifier.appClickable(onClick = onClick, minTouchTarget = 0.dp) else Modifier,
        ),
        shape = AppTheme.shapes.md,
        color = colors.surface,
        border = BorderStroke(AppTheme.sizes.borderWidth, colors.border),
    ) {
        Column(
            modifier = Modifier.padding(AppTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                icon?.let {
                    AppIcon(it, contentDescription = null, tint = colors.contentTertiary, size = 16.dp)
                }
                AppText(
                    text = label,
                    style = AppTheme.typography.labelSmall,
                    color = colors.contentTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            AppText(
                text = value,
                style = AppTheme.typography.displaySmall,
                color = colors.contentPrimary,
                maxLines = 1,
            )

            delta?.let {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIcon(
                        imageVector = if (deltaIsPositive) AppIcons.ArrowUp else AppIcons.ArrowDown,
                        contentDescription = if (deltaIsPositive) "Up" else "Down",
                        tint = deltaColors.content,
                        size = 14.dp,
                    )
                    AppText(
                        text = it,
                        style = AppTheme.typography.labelSmall,
                        color = deltaColors.content,
                    )
                }
            }
        }
    }
}

/**
 * A heading over a group, with an optional action on the right.
 *
 * The action is a text button rather than an icon: "See all" is unambiguous where an arrow glyph
 * beside a heading is read as decoration about half the time.
 */
@Composable
fun AppSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = AppTheme.spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            AppText(
                text = title,
                style = AppTheme.typography.headingSmall,
                color = AppTheme.colors.contentPrimary,
            )
            subtitle?.let {
                AppText(
                    text = it,
                    style = AppTheme.typography.caption,
                    color = AppTheme.colors.contentTertiary,
                )
            }
        }
        if (actionLabel != null && onAction != null) {
            AppText(
                text = actionLabel,
                modifier = Modifier.appClickable(onClick = onAction, minTouchTarget = 0.dp)
                    .padding(horizontal = AppTheme.spacing.sm, vertical = AppTheme.spacing.xs),
                style = AppTheme.typography.titleSmall,
                color = AppTheme.colors.accent,
            )
        }
    }
}

/**
 * A star rating, with real half stars.
 *
 * A fractional rating is the normal case — an average of other people's ratings almost never
 * lands on a whole number — and rounding 3.5 up to four stars erases exactly the difference the
 * number exists to express. The half is drawn by clipping a filled star over an outline one, so
 * there is no third icon to keep in step with the other two.
 *
 * Read-only unless [onRatingChange] is given, and the read-only form clears its semantics and
 * announces the value as text — five separate star images is not what a screen reader user needs
 * to hear.
 */
@Composable
fun AppRating(
    rating: Float,
    modifier: Modifier = Modifier,
    max: Int = 5,
    starSize: Dp = 18.dp,
    onRatingChange: ((Int) -> Unit)? = null,
) {
    val colors = AppTheme.colors
    val description = "$rating out of $max"

    Row(
        modifier = modifier.semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        repeat(max) { index ->
            val fill = (rating - index).coerceIn(0f, 1f)
            val tapModifier = if (onRatingChange != null) {
                Modifier.appClickable(
                    onClick = { onRatingChange(index + 1) },
                    minTouchTarget = 0.dp,
                )
            } else {
                Modifier
            }

            Box(
                modifier = Modifier
                    .clearAndSetSemantics {}
                    .then(tapModifier),
            ) {
                AppIcon(
                    imageVector = if (fill >= FULL_STAR) AppIcons.StarFilled else AppIcons.Star,
                    contentDescription = null,
                    tint = if (fill >= FULL_STAR) colors.warning.content else colors.borderStrong,
                    size = starSize,
                )

                if (fill >= HALF_STAR && fill < FULL_STAR) {
                    AppIcon(
                        imageVector = AppIcons.StarFilled,
                        contentDescription = null,
                        tint = colors.warning.content,
                        size = starSize,
                        modifier = Modifier.drawWithContent {
                            clipRect(right = size.width / 2f) { this@drawWithContent.drawContent() }
                        },
                    )
                }
            }
        }
    }
}

/** Above this much of a star is filled, it reads as whole; above half, as a half. */
private const val FULL_STAR = 0.75f
private const val HALF_STAR = 0.25f

/**
 * A tiny chart, sized to sit inside a row or a stat tile.
 *
 * Deliberately not a charting library. It draws one series with no axes, no legend and no
 * tooltip, because that is what a sparkline *is* — the moment a chart needs an axis it needs a
 * real chart, and pretending otherwise produces something that is bad at both jobs.
 *
 * Values are normalised against their own range, so a flat series renders as a centred line
 * rather than dividing by zero.
 */
@Composable
fun AppSparkline(
    values: List<Float>,
    modifier: Modifier = Modifier,
    color: Color = AppTheme.colors.accent,
    strokeWidth: Dp = 2.dp,
    height: Dp = 48.dp,
    fill: Boolean = true,
) {
    if (values.size < 2) return

    val progress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = AppTheme.motion.navigation(),
        label = "sparkline",
    )

    // The height is a parameter rather than something the caller puts in the modifier, because a
    // Canvas is a Spacer: it measures to zero on any axis whose constraint is not *fixed*, so a
    // minimum height silently does nothing and the chart draws as a flat line inside a box that
    // did grow. Only `height()` is a fixed constraint.
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
                    listOf(color.copy(alpha = FILL_ALPHA), Color.Transparent),
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

/**
 * A horizontal bar chart, one row per value.
 *
 * Rows rather than vertical columns, because a category label fits beside a horizontal bar and
 * has to be rotated or truncated under a vertical one — and on a phone there is far more vertical
 * room than horizontal.
 */
@Composable
fun AppBarChart(
    entries: List<Pair<String, Float>>,
    modifier: Modifier = Modifier,
    barColor: Color = AppTheme.colors.accent,
    trackColor: Color = AppTheme.colors.surfaceVariant,
    barHeight: Dp = 10.dp,
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
                animationSpec = AppTheme.motion.navigation(),
                label = "bar",
            )

            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    AppText(
                        text = label,
                        style = AppTheme.typography.bodySmall,
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

/**
 * A dense two-column grid for records, where a list row would waste half the width.
 *
 * Zebra striping rather than dividers: at this density a rule between every row is more ink than
 * the data, and the alternating fill keeps a long row readable across the screen.
 */
@Composable
fun AppKeyValueGrid(
    entries: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
    striped: Boolean = true,
) {
    val colors = AppTheme.colors

    Column(modifier = modifier.fillMaxWidth()) {
        entries.forEachIndexed { index, (key, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (striped && index % 2 == 1) colors.surfaceVariant else Color.Transparent,
                    )
                    .padding(horizontal = AppTheme.spacing.md, vertical = AppTheme.spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
            ) {
                AppText(
                    text = key,
                    modifier = Modifier.weight(1f),
                    style = AppTheme.typography.bodySmall,
                    color = colors.contentTertiary,
                )
                AppText(
                    text = value,
                    modifier = Modifier.weight(1f),
                    style = AppTheme.typography.bodySmall,
                    color = colors.contentPrimary,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

private const val LABEL_WEIGHT = 1f
private const val VALUE_WEIGHT = 1.4f
private const val FILL_ALPHA = 0.22f
