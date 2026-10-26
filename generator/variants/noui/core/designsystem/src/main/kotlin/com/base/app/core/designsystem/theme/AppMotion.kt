package com.base.app.core.designsystem.theme

import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.base.app.core.designsystem.theme.tokens.DesignTokens
import kotlin.math.PI

/**
 * One spring in the terms iOS designs with: how long it takes to feel settled ([response], in
 * seconds) and how far it overshoots ([bounce], 0 for none, negative for overdamped). With mass 1
 * that is `stiffness = (2π / response)²` and `dampingRatio = 1 − bounce`.
 *
 * The specs are built once, with visibility thresholds, so the invisible tail of a settle stops
 * drawing and nothing allocates a spec per recomposition.
 */
@Immutable
public class AppSpring(public val response: Float, public val bounce: Float) {
    internal constructor(token: DesignTokens.Spring) : this(token.response, token.bounce)

    public val stiffness: Float = ((2 * PI / response) * (2 * PI / response)).toFloat()
    public val dampingRatio: Float = 1f - bounce

    /** For values on a 0 to 1 scale: a press, a scale, a progress. */
    public val unit: SpringSpec<Float> =
        spring(dampingRatio, stiffness, DesignTokens.Motion.visibilityThreshold)

    /** For a distance in pixels held as a float. */
    public val pixels: SpringSpec<Float> =
        spring(dampingRatio, stiffness, visibilityThreshold = 0.5f)

    public val offset: SpringSpec<IntOffset> =
        spring(dampingRatio, stiffness, IntOffset.VisibilityThreshold)
    public val size: SpringSpec<IntSize> =
        spring(dampingRatio, stiffness, IntSize.VisibilityThreshold)
    public val dp: SpringSpec<Dp> = spring(dampingRatio, stiffness, Dp.VisibilityThreshold)
}

/**
 * Every spring, decay and fade the app animates with, built once from `DesignTokens.Motion`. Read
 * through `AppTheme.springs`. Full-screen moves never bounce: a long travel with any overshoot in it
 * reads as unstable. A value only one component animates with belongs in that component's
 * `XDefaults`, not here.
 */
@Immutable
public class AppSprings internal constructor() {
    private val tokens = DesignTokens.Motion

    /** Finger down. Quick and flat, so the press is seen before the finger hides it. */
    public val press: AppSpring = AppSpring(tokens.press)

    /** Finger up: the one place a spring may overshoot. */
    public val release: AppSpring = AppSpring(tokens.release)

    /** Thumbs, page dots and anything else moving to a new choice. Never bounces. */
    public val thumb: AppSpring = AppSpring(tokens.thumb)

    /** Push, pop and full-screen covers. */
    public val screen: AppSpring = AppSpring(tokens.screen)

    /** Sheets and their detents, toasts, a search field's focus. */
    public val sheet: AppSpring = AppSpring(tokens.sheet)

    /** Alerts, menus and popovers arriving: quicker than a sheet, never bouncing. */
    public val dialog: AppSpring = AppSpring(tokens.dialog)

    /** List placement, expanding and collapsing. */
    public val layout: AppSpring = AppSpring(tokens.layout)

    /** Pop-ins, badges and success marks. */
    public val emphasis: AppSpring = AppSpring(tokens.emphasis)

    /** A value trailing the finger, such as a lifted row: quick enough to feel attached. */
    public val interactive: AppSpring = AppSpring(tokens.interactive)

    /**
     * Content coming back from past an edge: a pull to refresh settling, a fling caught at the end of
     * a list. Overdamped, so it carries the finger's velocity in without overshooting the edge.
     */
    public val bounceBack: AppSpring = AppSpring(tokens.bounceBack)

    /**
     * A fling's deceleration, as iOS scroll views slow down (0.998 of the velocity kept per
     * millisecond). [flingFast] is the paging and picker rate (0.99), which stops much sooner.
     * Both end once the content no longer visibly moves.
     */
    public val fling: DecayAnimationSpec<Float> =
        exponentialDecay(tokens.flingFriction, FlingRestVelocity)
    public val flingFast: DecayAnimationSpec<Float> =
        exponentialDecay(tokens.pagingFriction, FlingRestVelocity)

    /** Colour and opacity only; anything that moves uses a spring. */
    public val fadeQuick: Int = tokens.fadeShortMs
    public val fadeSlow: Int = tokens.fadeLongMs

    /** Arrivals decelerate hard, so they read as settled rather than coasting. */
    public val enterEasing: Easing = tokens.fadeEnterEasing
    public val exitEasing: Easing = tokens.fadeExitEasing

    public fun <T> fadeIn(): FiniteAnimationSpec<T> = tween(fadeSlow, easing = enterEasing)
    public fun <T> fadeOut(): FiniteAnimationSpec<T> = tween(fadeQuick, easing = exitEasing)
}

/**
 * The speed, in pixels a second, at which a fling has come to rest: a quarter of a pixel a frame at
 * 120 Hz. An exponential decay never reaches zero; at Compose's default threshold of 0.1 a gentle
 * release would keep a list scrolling, invisibly, for four seconds and more, and a tap in that time
 * would only stop the fling instead of reaching the row under it.
 */
private const val FlingRestVelocity = 30f

/** The app's springs, built once per process. */
internal val DefaultSprings = AppSprings()
