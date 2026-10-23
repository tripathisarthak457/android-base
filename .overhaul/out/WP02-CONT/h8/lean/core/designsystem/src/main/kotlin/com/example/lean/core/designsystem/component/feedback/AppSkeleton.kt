package com.example.lean.core.designsystem.component.feedback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.lean.core.designsystem.foundation.draw.TextBones
import com.example.lean.core.designsystem.foundation.draw.lineSkeleton
import com.example.lean.core.designsystem.foundation.draw.skeleton
import com.example.lean.core.designsystem.theme.AppTheme

/**
 * A placeholder block in [shape], for content that has no layout to redact: a chart, a map tile, an
 * image. For text, rows and cards, prefer [AppRedacted] around the real composable given placeholder
 * data: it keeps the content's size at every font scale, so nothing moves when the data arrives.
 *
 * One band sweeps across every placeholder in the window. With motion reduced, in a hidden tab, in a
 * preview or outside `AppTheme`, the placeholder holds still. Hidden from screen readers: the
 * loading region announces itself once.
 */
@Composable
fun AppSkeleton(
    modifier: Modifier = Modifier,
    shape: Shape = AppSkeletonDefaults.shape,
) {
    Spacer(modifier.clearAndSetSemantics {}.skeleton(shape))
}

/**
 * A placeholder line of text in [style]: as tall as one line of that style at the current font
 * scale, with the bone where `AppText` would draw the letters, across [widthFraction] of the width
 * it is given. Lines of a redacted `AppText` are drawn the same way.
 */
@Composable
fun AppSkeletonLine(
    modifier: Modifier = Modifier,
    widthFraction: Float = 1f,
    style: TextStyle = AppTheme.typography.body,
) {
    val measurer = rememberTextMeasurer(cacheSize = 0)
    val layout = remember(measurer, style) { measurer.measure(LineSample, style, maxLines = 1) }
    // The bone of a one-line text, which is how a redacted AppText draws the same line.
    val bone = remember(layout) { TextBones().apply { update(layout) } }
    Spacer(
        modifier
            .fillMaxWidth(widthFraction)
            .height(with(LocalDensity.current) { layout.size.height.toDp() })
            .clearAndSetSemantics {}
            .lineSkeleton(top = bone.top(0), bottom = bone.bottom(0)),
    )
}

/** A placeholder line [height] tall at every font scale, which the text it stands for is not. */
@Deprecated(
    "A fixed height ignores the font scale. Pass the text style the line stands for.",
    ReplaceWith("AppSkeletonLine(modifier, widthFraction)"),
    DeprecationLevel.WARNING,
)
@Composable
fun AppSkeletonLine(
    modifier: Modifier = Modifier,
    widthFraction: Float = 1f,
    height: Dp,
) {
    AppSkeleton(modifier = modifier.fillMaxWidth(widthFraction).height(height), shape = AppTheme.shapes.xs)
}

/** A round placeholder, for an avatar or an icon in a circle. */
@Composable
fun AppSkeletonCircle(
    modifier: Modifier = Modifier,
    size: Dp = AppSkeletonDefaults.CircleSize,
) {
    AppSkeleton(modifier = modifier.size(size), shape = CircleShape)
}

/** A placeholder shaped like a list row with a leading avatar and two lines. */
@Deprecated(
    "Its shape matches no real row, so content jumps when it arrives. Redact the real row instead.",
    ReplaceWith("AppRedacted(active = true) { /* the row, given placeholder data */ }"),
    DeprecationLevel.WARNING,
)
@Composable
fun AppSkeletonListItem(modifier: Modifier = Modifier, showLeading: Boolean = true) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = AppTheme.spacing.lg, vertical = AppTheme.spacing.md),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showLeading) AppSkeletonCircle()
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
            // Its layout stays as it was until it is removed, fixed heights included.
            @Suppress("DEPRECATION")
            AppSkeletonLine(widthFraction = 0.6f, height = 14.dp)
            @Suppress("DEPRECATION")
            AppSkeletonLine(widthFraction = 0.9f, height = 12.dp)
        }
    }
}

/** What a skeleton looks like when the caller does not say. */
object AppSkeletonDefaults {
    /** The corners of a block placeholder: the design style's small radius. */
    val shape: Shape
        @Composable
        @ReadOnlyComposable
        get() = AppTheme.shapes.sm

    /** The diameter of [AppSkeletonCircle]: an avatar's. */
    val CircleSize: Dp = 40.dp
}

/** Capitals and descenders, so the measured line is as tall as any real one in the style. */
private const val LineSample = "Hg"
