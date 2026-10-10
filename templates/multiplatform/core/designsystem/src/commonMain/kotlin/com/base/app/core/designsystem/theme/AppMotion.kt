package com.base.app.core.designsystem.theme

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import com.base.app.core.designsystem.theme.tokens.DesignTokens

/**
 * How the app moves, as one choice: how much a release overshoots, and how quickly things travel.
 * The numbers are in `DesignTokens.Motion`.
 */
enum class AppMotionStyle {
    /** A trace of overshoot on release. Lively without drawing attention to itself. */
    Standard,

    /** A visible overshoot on release. Playful; the wrong choice for anything handling money. */
    Bouncy,

    /** No overshoot anywhere and slightly slower travel. Dense, professional interfaces. */
    Calm,

    /** Quicker travel and a hint of overshoot. Utilities used dozens of times a day. */
    Snappy,
    ;

    internal fun tokens(): DesignTokens.MotionStyle = when (this) {
        Standard -> DesignTokens.Motion.standard
        Bouncy -> DesignTokens.Motion.bouncy
        Calm -> DesignTokens.Motion.calm
        Snappy -> DesignTokens.Motion.snappy
    }
}

/**
 * One spring, with its specs built once, visibility thresholds included, so the invisible tail of
 * a settle stops drawing and nothing allocates a spec per recomposition.
 */
@Immutable
class AppSpring internal constructor(token: DesignTokens.Spring) {
    val stiffness: Float = token.stiffness
    val dampingRatio: Float = token.dampingRatio

    /** For values on a 0 to 1 scale: a press, a scale, a progress. */
    val unit: SpringSpec<Float> = spring(dampingRatio, stiffness, DesignTokens.Motion.visibilityThreshold)

    /** For a distance in pixels held as a float. */
    val pixels: SpringSpec<Float> = spring(dampingRatio, stiffness, visibilityThreshold = 0.5f)

    val offset: SpringSpec<IntOffset> = spring(dampingRatio, stiffness, IntOffset.VisibilityThreshold)
    val dp: SpringSpec<Dp> = spring(dampingRatio, stiffness, Dp.VisibilityThreshold)
}

/** How a screen recedes when another covers it. */
@Immutable
class NavigationMotion internal constructor() {
    /** How far the covered screen travels, as a fraction of the one arriving over it. */
    val parallax: Float = DesignTokens.Motion.navigationParallax
    val scrimAlphaLight: Float = DesignTokens.Motion.navigationScrimAlphaLight
    val scrimAlphaDark: Float = DesignTokens.Motion.navigationScrimAlphaDark
}

/**
 * Every spring the app animates with, precomputed for one motion style. Read through
 * `AppTheme.springs`; a raw `tween()` or `spring()` in a screen is refused by review.
 */
@Immutable
class AppSprings internal constructor(style: AppMotionStyle) {
    private val tokens = style.tokens()

    /** Finger down. Quick and flat, so the press is seen before the finger hides it. */
    val press = AppSpring(tokens.press)

    /** Finger up: the one place most styles overshoot. */
    val release = AppSpring(tokens.release)

    /** A selection moving to a new choice: the tab bar's indicator, a checkmark. */
    val selection = AppSpring(tokens.selection)

    /** Push, pop and full-screen covers. Never bounces in any style. */
    val screen = AppSpring(tokens.screen)

    /** Sheets and their detents. */
    val sheet = AppSpring(tokens.sheet)

    /** List placement, expanding and collapsing. */
    val layout = AppSpring(tokens.layout)

    /** Badges, success marks: the few things that should be noticed. */
    val emphasis = AppSpring(tokens.emphasis)

    val navigation = NavigationMotion()
}

private val SpringsByStyle = AppMotionStyle.entries.associateWith(::AppSprings)

/** The springs for [style], built once per process. */
fun AppMotionStyle.springs(): AppSprings = SpringsByStyle.getValue(this)

internal val LocalAppSprings = staticCompositionLocalOf { AppMotionStyle.Standard.springs() }
