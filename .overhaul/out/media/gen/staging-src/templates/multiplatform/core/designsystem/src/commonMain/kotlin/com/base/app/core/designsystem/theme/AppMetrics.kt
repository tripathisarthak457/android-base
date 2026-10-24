package com.base.app.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.base.app.core.designsystem.theme.tokens.DesignTokens

/** The spacing scale. A 4dp grid, with the two values you reach for most given their own names. */
@Immutable
data class AppSpacing(
    val xxs: Dp = DesignTokens.Spacing.xxs,
    val xs: Dp = DesignTokens.Spacing.xs,
    val sm: Dp = DesignTokens.Spacing.sm,
    val md: Dp = DesignTokens.Spacing.md,
    val lg: Dp = DesignTokens.Spacing.lg,
    val xl: Dp = DesignTokens.Spacing.xl,
    val xxl: Dp = DesignTokens.Spacing.xxl,
    val xxxl: Dp = DesignTokens.Spacing.xxxl,

    /** The horizontal inset from the pane's edge to content. It follows the pane's width class. */
    val gutter: Dp = DesignTokens.Spacing.gutter,

    /** The vertical gap between two sections of a screen. */
    val section: Dp = DesignTokens.Spacing.section,

    /** The gap between stacked cards or rows in a list. */
    val stack: Dp = DesignTokens.Spacing.stack,
)

/**
 * Corner radii. [pill] is a very large radius rather than a `CircleShape`, so it stays correct on
 * a component that is wider than it is tall.
 */
@Immutable
class AppShapes internal constructor() {
    val sm: Shape = RoundedCornerShape(DesignTokens.Radii.sm)
    val md: Shape = RoundedCornerShape(DesignTokens.Radii.md)
    val lg: Shape = RoundedCornerShape(DesignTokens.Radii.lg)
    val pill: Shape = RoundedCornerShape(percent = 50)
}

/**
 * Fixed sizes: the touch target every control reserves however small it draws, icon sizes, and the
 * heights components start from. Text-bearing components treat their height as a minimum, so
 * they grow with the person's font size.
 */
@Immutable
class AppSizes internal constructor() {
    val minTouchTarget: Dp = DesignTokens.Sizes.minTouchTarget
    val iconSmall: Dp = DesignTokens.Sizes.iconSmall
    val icon: Dp = DesignTokens.Sizes.icon
    val iconLarge: Dp = DesignTokens.Sizes.iconLarge
    val buttonSmall: Dp = DesignTokens.Sizes.buttonSmall
    val buttonMedium: Dp = DesignTokens.Sizes.buttonMedium
    val buttonLarge: Dp = DesignTokens.Sizes.buttonLarge
    val fieldHeight: Dp = DesignTokens.Sizes.fieldHeight
    val topBarHeight: Dp = DesignTokens.Sizes.topBarHeight
    val tabBarHeight: Dp = DesignTokens.Sizes.tabBarHeight
}

internal val LocalAppSpacing = staticCompositionLocalOf { AppSpacing() }
internal val DefaultShapes = AppShapes()
internal val DefaultSizes = AppSizes()
