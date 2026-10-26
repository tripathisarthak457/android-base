package com.example.cmp.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.example.cmp.core.designsystem.theme.tokens.DesignTokens

/** The spacing scale. A 4dp grid, with the two values you reach for most given their own names. */
@Immutable
public data class AppSpacing(
    val xxs: Dp = DesignTokens.Spacing.xxs,
    val xs: Dp = DesignTokens.Spacing.xs,
    val sm: Dp = DesignTokens.Spacing.sm,
    val md: Dp = DesignTokens.Spacing.md,
    val lg: Dp = DesignTokens.Spacing.lg,
    val xl: Dp = DesignTokens.Spacing.xl,
    val xxl: Dp = DesignTokens.Spacing.xxl,
    val xxxl: Dp = DesignTokens.Spacing.xxxl,

    /** The horizontal inset from the window edge to content. One value, app-wide. */
    val gutter: Dp = DesignTokens.Spacing.gutter,

    /** The vertical gap between two sections of a screen. */
    val section: Dp = DesignTokens.Spacing.section,

    /** The gap between stacked cards or rows in a list. */
    val stack: Dp = DesignTokens.Spacing.stack,
)

/**
 * Corner radii. [pill] is a very large radius rather than a `CircleShape`, so it stays correct on a
 * component that is wider than it is tall — a circle on a 200×40 chip clips the label.
 */
@Immutable
public data class AppShapes(
    val none: Shape = RoundedCornerShape(DesignTokens.Radii.none),
    val xs: Shape = RoundedCornerShape(DesignTokens.Radii.xs),
    val sm: Shape = RoundedCornerShape(DesignTokens.Radii.sm),
    val md: Shape = RoundedCornerShape(DesignTokens.Radii.md),
    val lg: Shape = RoundedCornerShape(DesignTokens.Radii.lg),
    val xl: Shape = RoundedCornerShape(DesignTokens.Radii.xl),
    val pill: Shape = RoundedCornerShape(percent = 50),
)

/** Elevation, as a token rather than a raw `Dp` at the call site. */
@Immutable
public data class AppElevation(
    val none: Dp = DesignTokens.Elevation.none,
    val raised: Dp = DesignTokens.Elevation.raised,
    val card: Dp = DesignTokens.Elevation.card,
    val overlay: Dp = DesignTokens.Elevation.overlay,
    val modal: Dp = DesignTokens.Elevation.modal,
)

/**
 * Fixed sizes: the 48dp touch target every control reserves however small it draws, icon sizes, and
 * the heights components start from. Text-bearing components treat their height as a minimum, so
 * they grow with the user's font size.
 */
@Immutable
public data class AppSizes(
    val minTouchTarget: Dp = DesignTokens.Sizes.minTouchTarget,
    val iconSmall: Dp = DesignTokens.Sizes.iconSmall,
    val icon: Dp = DesignTokens.Sizes.icon,
    val iconLarge: Dp = DesignTokens.Sizes.iconLarge,
    val buttonSmall: Dp = DesignTokens.Sizes.buttonSmall,
    val buttonMedium: Dp = DesignTokens.Sizes.buttonMedium,
    val buttonLarge: Dp = DesignTokens.Sizes.buttonLarge,
    val fieldHeight: Dp = DesignTokens.Sizes.fieldHeight,
    val rowHeight: Dp = DesignTokens.Sizes.rowHeight,
    val topBarHeight: Dp = DesignTokens.Sizes.topBarHeight,
)

internal val LocalAppSpacing = staticCompositionLocalOf { AppSpacing() }
