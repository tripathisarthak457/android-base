package com.example.wp04.everything.core.designsystem.theme

import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.wp04.everything.core.designsystem.theme.tokens.DesignTokens
import kotlin.math.PI

/**
 * How the app moves, as one choice: how much a release overshoots, and how quickly things travel.
 * The numbers are in `DesignTokens.Motion`.
 *
 * ```
 * AppTheme(motionStyle = AppMotionStyle.Calm) { … }
 * ```
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

    /** One line saying what the style feels like, for pickers and the catalog. */
    val summary: String
        get() = tokens().summary

    internal fun tokens(): DesignTokens.MotionStyle = when (this) {
        Standard -> DesignTokens.Motion.standard
        Bouncy -> DesignTokens.Motion.bouncy
        Calm -> DesignTokens.Motion.calm
        Snappy -> DesignTokens.Motion.snappy
    }
}

/**
 * One spring in the terms iOS designs with: how long it takes to feel settled ([response], in
 * seconds) and how far it overshoots ([bounce], 0 for none, negative for overdamped). With mass 1
 * that is `stiffness = (2π / response)²` and `dampingRatio = 1 − bounce`.
 *
 * The specs are built once, with visibility thresholds, so the invisible tail of a settle stops
 * drawing and nothing allocates a spec per recomposition.
 */
@Immutable
class AppSpring(val response: Float, val bounce: Float) {
    internal constructor(token: DesignTokens.Spring) : this(token.response, token.bounce)

    val stiffness: Float = ((2 * PI / response) * (2 * PI / response)).toFloat()
    val dampingRatio: Float = 1f - bounce

    /** For values on a 0 to 1 scale: a press, a scale, a progress. */
    val unit: SpringSpec<Float> = spring(dampingRatio, stiffness, DesignTokens.Motion.visibilityThreshold)

    /** For a distance in pixels held as a float. */
    val pixels: SpringSpec<Float> = spring(dampingRatio, stiffness, visibilityThreshold = 0.5f)

    val offset: SpringSpec<IntOffset> = spring(dampingRatio, stiffness, IntOffset.VisibilityThreshold)
    val size: SpringSpec<IntSize> = spring(dampingRatio, stiffness, IntSize.VisibilityThreshold)
    val dp: SpringSpec<Dp> = spring(dampingRatio, stiffness, Dp.VisibilityThreshold)
}

/** How a screen recedes when another covers it, and when the shell lets a held Back through. */
@Immutable
class NavigationMotion internal constructor() {
    /** How far the covered screen travels, as a fraction of the one arriving over it. */
    val parallax: Float = DesignTokens.Motion.navigationParallax
    val scrimAlphaLight: Float = DesignTokens.Motion.navigationScrimAlphaLight
    val scrimAlphaDark: Float = DesignTokens.Motion.navigationScrimAlphaDark

    /** The soft shadow along the arriving screen's leading edge: its width and its darkest alpha. */
    val edgeShadow: Dp = DesignTokens.Motion.navigationEdgeShadow
    val edgeShadowAlpha: Float = DesignTokens.Motion.navigationEdgeShadowAlpha

    /**
     * The fraction of a transition's travel after which Back presses counted during it are applied
     * as one pop. Earlier than this, the pop that follows starts from rest and the screens jump.
     */
    val backGateRelease: Float = DesignTokens.Motion.navigationBackGateRelease
}

/**
 * The tab bar's motion. Switching tabs nudges the incoming tab [revealShift] toward the travel and
 * lifts a [revealVeilAlpha] veil off it, both on `thumb`; the indicator stretches between its two
 * edges up to [indicatorMaxStretch] slots and squashes its height by [indicatorSquash] per slot of
 * stretch; the selected glyph dips to [iconBounceScale] and springs back; the bar minimizes after
 * [minimizeHysteresis] of scrolling one way.
 */
@Immutable
class TabBarMotion internal constructor() {
    val revealShift: Dp = DesignTokens.Motion.tabRevealShift
    val revealVeilAlpha: Float = DesignTokens.Motion.tabRevealVeilAlpha
    val indicatorHeight: Dp = DesignTokens.Motion.tabIndicatorHeight
    val indicatorMaxStretch: Float = DesignTokens.Motion.tabIndicatorMaxStretch
    val indicatorSquash: Float = DesignTokens.Motion.tabIndicatorSquash
    val iconBounceScale: Float = DesignTokens.Motion.tabIconBounceScale
    val minimizeHysteresis: Dp = DesignTokens.Motion.tabMinimizeHysteresis
}

/**
 * Every spring, fade and press value the app animates with, precomputed for one motion style. Read
 * through `AppTheme.springs`. Full-screen moves never bounce, in any style: a long travel with any
 * overshoot in it reads as unstable.
 */
@Immutable
class AppSprings internal constructor(val style: AppMotionStyle) {
    private val tokens = style.tokens()

    /** Finger down. Quick and flat, so the press is seen before the finger hides it. */
    val press = AppSpring(tokens.press)

    /** Finger up: the one place most styles overshoot. */
    val release = AppSpring(tokens.release)

    /**
     * Thumbs, page dots and anything else moving to a new choice: a switch, a segmented control,
     * the tab reveal and the bar minimizing. Never bounces: overshoot belongs to the release.
     */
    val thumb = AppSpring(tokens.thumb)

    /**
     * The tab indicator's two edges. The edge in front leads on a quicker spring with the style's
     * bounce and the one behind follows flat, so the pill stretches toward its target and settles.
     */
    val indicatorLead = AppSpring(tokens.indicatorLead)
    val indicatorTrail = AppSpring(tokens.indicatorTrail)

    /** Push, pop, full-screen covers and the back card's commit. */
    val screen = AppSpring(tokens.screen)

    /** Sheets and their detents, toasts, the search field's focus. */
    val sheet = AppSpring(tokens.sheet)

    /** Alerts, action sheets, menus and popovers arriving: quicker than a sheet, never bouncing. */
    val dialog = AppSpring(tokens.dialog)

    /** List placement, expanding and collapsing. */
    val layout = AppSpring(tokens.layout)

    /** Pop-ins, badges and success marks. */
    val emphasis = AppSpring(tokens.emphasis)

    /** A value trailing the finger, such as a lifted row: quick enough to feel attached. */
    val interactive = AppSpring(tokens.interactive)

    /**
     * Content coming back from past an edge: a pull to refresh settling, a fling caught at the end of
     * a list. Overdamped, so it carries the finger's velocity in without overshooting the edge.
     */
    val bounceBack = AppSpring(tokens.bounceBack)

    /** A glyph's bounce, as SF Symbols plays it: a quick flat dip, then a springy return. */
    val iconBounceDown = AppSpring(tokens.iconBounceDown)
    val iconBounceUp = AppSpring(tokens.iconBounceUp)

    /**
     * A rubber band let go with no velocity, in pixels. So overdamped it is a decay: it closes by half
     * every 70 ms without ever travelling past the edge.
     */
    val overscrollRelease: SpringSpec<Float> = spring(
        dampingRatio = DesignTokens.Motion.overscrollReleaseDampingRatio,
        stiffness = DesignTokens.Motion.overscrollReleaseStiffness,
        visibilityThreshold = 0.5f,
    )

    /**
     * How hard content resists being pulled past an edge: iOS's `c` in `c·x·d / (d + c·x)`, the
     * same for overscroll, pull to refresh, sheets and swipe actions.
     */
    val rubberBand: Float = DesignTokens.Motion.rubberBand

    /**
     * A fling's deceleration, as iOS scroll views slow down (0.998 of the velocity kept per
     * millisecond). [flingFast] is the paging and picker rate (0.99), which stops much sooner.
     * Both end once the content no longer visibly moves.
     */
    val fling: DecayAnimationSpec<Float> = exponentialDecay(DesignTokens.Motion.flingFriction, FlingRestVelocity)
    val flingFast: DecayAnimationSpec<Float> = exponentialDecay(DesignTokens.Motion.pagingFriction, FlingRestVelocity)

    /** Colour and opacity only; anything that moves uses a spring. */
    val fadeQuick: Int = DesignTokens.Motion.fadeShortMs
    val fadeSlow: Int = DesignTokens.Motion.fadeLongMs

    /** Arrivals decelerate hard, so they read as settled rather than coasting. */
    val enterEasing: Easing = DesignTokens.Motion.fadeEnterEasing
    val exitEasing: Easing = DesignTokens.Motion.fadeExitEasing

    fun <T> fadeIn(): FiniteAnimationSpec<T> = tween(fadeSlow, easing = enterEasing)
    fun <T> fadeOut(): FiniteAnimationSpec<T> = tween(fadeQuick, easing = exitEasing)

    /** The delay between items of a list appearing together, how many wait their turn, and how far they rise. */
    val staggerMillis: Int = DesignTokens.Motion.staggerStepMs
    val staggerMaxItems: Int = DesignTokens.Motion.staggerMaxItems
    val appearOffset: Dp = DesignTokens.Motion.staggerOffset

    /** A pressed button or chip. */
    val pressScale: Float = DesignTokens.Motion.pressScale

    /** A pressed card or anything wide, where 0.96 reads as the layout jumping. */
    val pressScaleLarge: Float = DesignTokens.Motion.pressScaleLarge

    /** A pressed icon-sized control, which has to travel further to be seen around the finger. */
    val pressScaleCompact: Float = DesignTokens.Motion.pressScaleIcon

    /** A pressed text or icon button. */
    val pressDimAlpha: Float = DesignTokens.Motion.pressDimAlpha

    /** How long a pressed row's highlight takes to fade after the finger lifts. It appears at once. */
    val highlightFadeOut: Int = DesignTokens.Motion.highlightFadeOutMs

    val navigation = NavigationMotion()

    val tabBar = TabBarMotion()
}

/**
 * The speed, in pixels a second, at which a fling has come to rest: a quarter of a pixel a frame at
 * 120 Hz. An exponential decay never reaches zero; at Compose's default threshold of 0.1 a gentle
 * release would keep a list scrolling, invisibly, for four seconds and more, and a tap in that time
 * would only stop the fling instead of reaching the row under it.
 */
private const val FlingRestVelocity = 30f

private val SpringsByStyle = AppMotionStyle.entries.associateWith(::AppSprings)

/** The springs for [style], built once per process. */
fun AppMotionStyle.springs(): AppSprings = SpringsByStyle.getValue(this)

internal val LocalAppSprings = staticCompositionLocalOf { AppMotionStyle.Standard.springs() }
