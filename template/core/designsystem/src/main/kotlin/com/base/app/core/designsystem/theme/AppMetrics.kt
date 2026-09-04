package com.base.app.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The spacing scale. A 4dp grid, with the two values you reach for most given their own names.
 *
 * Hard-coded `.dp` in a screen is how two lists end up with 14dp and 16dp gutters that nobody
 * notices individually and everybody feels collectively.
 */
@Immutable
data class AppSpacing(
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val xxxl: Dp = 48.dp,

    /** The horizontal inset from the window edge to content. One value, app-wide. */
    val gutter: Dp = 16.dp,

    /** The vertical gap between two sections of a screen. */
    val section: Dp = 24.dp,

    /** The gap between stacked cards or rows in a list. */
    val stack: Dp = 12.dp,
)

/**
 * Corner radii.
 *
 * [pill] is a very large radius rather than a `CircleShape`, so it stays correct on a component
 * that is wider than it is tall — a circle on a 200×40 chip clips the label.
 */
@Immutable
data class AppShapes(
    val none: Shape = RoundedCornerShape(0.dp),
    val xs: Shape = RoundedCornerShape(6.dp),
    val sm: Shape = RoundedCornerShape(10.dp),
    val md: Shape = RoundedCornerShape(14.dp),
    val lg: Shape = RoundedCornerShape(20.dp),
    val xl: Shape = RoundedCornerShape(28.dp),
    val pill: Shape = RoundedCornerShape(percent = 50),

    /** Bottom sheets: rounded at the top, square where they meet the window edge. */
    val sheet: Shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
)

/**
 * Elevation, as a token rather than a raw `Dp` at the call site.
 *
 * Only used in light theme — see `AppSurface`, which substitutes a border in dark, where a black
 * drop shadow on a near-black surface is invisible at best and a grey smear at worst.
 */
@Immutable
data class AppElevation(
    val none: Dp = 0.dp,
    val raised: Dp = 1.dp,
    val card: Dp = 3.dp,
    val overlay: Dp = 8.dp,
    val modal: Dp = 16.dp,
)

/**
 * Minimum interactive sizes.
 *
 * [minTouchTarget] is 48dp because that is the accessibility floor, and it is enforced by
 * `Modifier.minimumTouchTarget` in the components rather than left to each caller — a 24dp icon
 * button that is only 24dp of touch area is the single most common accessibility defect in a
 * hand-rolled design system.
 */
@Immutable
data class AppSizes(
    val minTouchTarget: Dp = 48.dp,
    val iconSmall: Dp = 16.dp,
    val icon: Dp = 20.dp,
    val iconLarge: Dp = 24.dp,
    val buttonSmall: Dp = 36.dp,
    val buttonMedium: Dp = 44.dp,
    val buttonLarge: Dp = 52.dp,
    val fieldHeight: Dp = 52.dp,
    val topBarHeight: Dp = 56.dp,
    val bottomBarHeight: Dp = 60.dp,
    val borderWidth: Dp = 1.dp,
    val borderWidthStrong: Dp = 1.5.dp,
)

internal val LocalAppSpacing = staticCompositionLocalOf { AppSpacing() }
internal val LocalAppShapes = staticCompositionLocalOf { AppShapes() }
internal val LocalAppElevation = staticCompositionLocalOf { AppElevation() }
internal val LocalAppSizes = staticCompositionLocalOf { AppSizes() }
