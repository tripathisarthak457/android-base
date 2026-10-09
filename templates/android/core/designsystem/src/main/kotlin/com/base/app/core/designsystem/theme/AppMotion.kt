package com.base.app.core.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.PI

/**
 * How the app moves, as one choice: how much a release overshoots ([bounce], 0 for none) and how
 * quickly things travel ([speed], dividing every travelling spring's response).
 *
 * ```
 * AppTheme(motionStyle = AppMotionStyle.Calm) { … }
 * ```
 */
enum class AppMotionStyle(val bounce: Float, val speed: Float, val summary: String) {
    Standard(0.15f, 1f, "A trace of overshoot on release. Lively without drawing attention to itself."),
    Bouncy(0.35f, 1f, "A visible overshoot on release. Playful; the wrong choice for anything handling money."),
    Calm(0f, 0.87f, "No overshoot anywhere and slightly slower travel. Dense, professional interfaces."),
    Snappy(0.10f, 1.15f, "Quicker travel and a hint of overshoot. Utilities used dozens of times a day."),
}

/**
 * One spring in the terms iOS designs with: how long it takes to feel settled ([response], in
 * seconds) and how far it overshoots ([bounce], 0 for none). With mass 1 that is
 * `stiffness = (2π / response)²` and `dampingRatio = 1 − bounce`.
 *
 * The specs are built once, with visibility thresholds, so the invisible tail of a settle stops
 * drawing and nothing allocates a spec per recomposition.
 */
@Immutable
class AppSpring(val response: Float, val bounce: Float) {
    val stiffness: Float = ((2 * PI / response) * (2 * PI / response)).toFloat()
    val dampingRatio: Float = 1f - bounce

    /** For values on a 0 to 1 scale: a press, a scale, a progress. */
    val unit: SpringSpec<Float> = spring(dampingRatio, stiffness, visibilityThreshold = 0.001f)

    /** For a distance in pixels held as a float. */
    val pixels: SpringSpec<Float> = spring(dampingRatio, stiffness, visibilityThreshold = 0.5f)

    val offset: SpringSpec<IntOffset> = spring(dampingRatio, stiffness, IntOffset.VisibilityThreshold)
    val size: SpringSpec<IntSize> = spring(dampingRatio, stiffness, IntSize.VisibilityThreshold)
    val dp: SpringSpec<Dp> = spring(dampingRatio, stiffness, Dp.VisibilityThreshold)
}

/** How a screen recedes when another covers it. */
@Immutable
class NavigationMotion(
    /** How far the covered screen travels, as a fraction of the one arriving over it. */
    val parallax: Float,
    val scrimAlphaLight: Float,
    val scrimAlphaDark: Float,
    /** The soft shadow along the arriving screen's leading edge. */
    val edgeShadow: Dp,
)

/**
 * Every spring, fade and press value the app animates with, precomputed for one motion style. Read
 * through `AppTheme.springs`. Full-screen moves never bounce, in any style: a long travel with any
 * overshoot in it reads as unstable.
 */
@Immutable
class AppSprings internal constructor(val style: AppMotionStyle) {
    /** Finger down. Quick and flat, so the press is seen before the finger hides it. */
    val press = AppSpring(response = 0.16f, bounce = 0f)

    /** Finger up: the one place most styles overshoot. */
    val release = AppSpring(response = 0.32f, bounce = style.bounce)

    /** Indicators, pills, thumbs and page dots moving to a new choice. */
    val selection = AppSpring(response = 0.30f, bounce = 0.6f * style.bounce)

    /** Push, pop and full-screen covers. */
    val screen = AppSpring(response = 0.24f / style.speed, bounce = 0f)

    /** Sheets, detents, menus and alerts. */
    val sheet = AppSpring(response = 0.34f / style.speed, bounce = 0f)

    /** List placement, expanding and collapsing. */
    val layout = AppSpring(response = 0.36f / style.speed, bounce = 0f)

    /** Pop-ins, badges and success marks. */
    val emphasis = AppSpring(response = 0.50f, bounce = style.bounce)

    /** Colour and opacity only; anything that moves uses a spring. */
    val fadeQuick: Int = 120
    val fadeSlow: Int = 200

    /** Arrivals decelerate hard, so they read as settled rather than coasting. */
    val enterEasing: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val exitEasing: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    fun <T> fadeIn(): FiniteAnimationSpec<T> = tween(fadeSlow, easing = enterEasing)
    fun <T> fadeOut(): FiniteAnimationSpec<T> = tween(fadeQuick, easing = exitEasing)

    /** The delay between items of a list appearing together, and how many of them wait their turn. */
    val staggerMillis: Int = 24
    val staggerMaxItems: Int = 6
    val appearOffset: Dp = 12.dp

    /** A pressed button or chip. */
    val pressScale: Float = 0.96f

    /** A pressed card or anything wide, where 0.96 reads as the layout jumping. */
    val pressScaleLarge: Float = 0.98f

    /** A pressed icon-sized control, which has to travel further to be seen around the finger. */
    val pressScaleCompact: Float = 0.90f

    /** A pressed text or icon button. */
    val pressDimAlpha: Float = 0.45f

    /** How long a pressed row's highlight takes to fade after the finger lifts. It appears at once. */
    val highlightFadeOut: Int = 220

    val navigation = NavigationMotion(
        parallax = 0.30f,
        scrimAlphaLight = 0.10f,
        scrimAlphaDark = 0.24f,
        edgeShadow = 8.dp,
    )
}

private val SpringsByStyle = AppMotionStyle.entries.associateWith(::AppSprings)

/** The springs for [style], built once per process. */
fun AppMotionStyle.springs(): AppSprings = SpringsByStyle.getValue(this)

internal val LocalAppSprings = staticCompositionLocalOf { AppMotionStyle.Standard.springs() }
