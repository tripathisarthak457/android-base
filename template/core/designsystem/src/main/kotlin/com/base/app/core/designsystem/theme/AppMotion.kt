package com.base.app.core.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * How the app moves, as one choice.
 *
 * The personality of an interface is carried almost entirely by two numbers — how far something
 * moves when you touch it, and how much it overshoots on the way back. Naming the combinations
 * means a project picks a feel once, in [AppTheme], instead of every component arriving at its
 * own by accident.
 *
 * ```
 * AppTheme(motionStyle = AppMotionStyle.Bouncy) { … }
 * ```
 */
enum class AppMotionStyle {
    /**
     * Springy, with a visible overshoot on release. Reads as playful and responsive; the right
     * choice for consumer apps, and the wrong one for anything that handles money seriously.
     */
    Bouncy,

    /** Crisp, a trace of overshoot. The default: lively without drawing attention to itself. */
    Standard,

    /**
     * No overshoot anywhere and slightly longer durations. For dense, professional interfaces
     * where motion should be legible rather than expressive.
     */
    Calm,

    /**
     * Everything at the shortest duration that still reads as motion. For utilities people use
     * dozens of times a day, where any animation eventually becomes a delay.
     */
    Snappy,
    ;

    fun motion(): AppMotion = when (this) {
        Bouncy -> AppMotion(
            instant = 90,
            quick = 160,
            medium = 280,
            slow = 440,
            pressScale = 0.94f,
            pressOvershoot = 1.045f,
            pressDamping = 0.42f,
            pressStiffness = 900f,
            sheetDamping = 0.68f,
            navigationDamping = 0.82f,
        )

        Standard -> AppMotion()

        Calm -> AppMotion(
            instant = 110,
            quick = 190,
            medium = 300,
            slow = 460,
            pressScale = 0.985f,
            pressOvershoot = 1f,
            pressDamping = 1f,
            pressStiffness = 1200f,
            sheetDamping = 1f,
            navigationDamping = 1f,
        )

        Snappy -> AppMotion(
            instant = 60,
            quick = 100,
            medium = 160,
            slow = 240,
            pressScale = 0.96f,
            pressOvershoot = 1.02f,
            pressDamping = 0.75f,
            pressStiffness = 2000f,
            sheetDamping = 0.95f,
            navigationDamping = 1f,
        )
    }
}

/**
 * Every duration, easing and spring the app animates with.
 *
 * ## Springs for anything a finger can interrupt
 *
 * A press, a drag, a sheet, a screen transition: these are all things the user can reverse
 * mid-flight. A spring carries the current velocity into the new target, so a flick that changes
 * direction halfway continues from where the thing actually is. A tween has to either snap or
 * play out an animation nobody is watching any more, and that difference is most of what
 * separates an interface that feels physical from one that feels like a slideshow.
 *
 * ## Tweens for anything that just appears
 *
 * A fade, a colour change, a skeleton shimmer — nothing to interrupt, and a duration is easier to
 * reason about and cheaper to run.
 *
 * ## Don't build one of these by hand
 *
 * Pick an [AppMotionStyle] instead. The fields are public so a project with a real motion spec
 * can express it, but four numbers chosen independently rarely agree with each other.
 */
@Immutable
data class AppMotion(
    val instant: Int = 90,
    val quick: Int = 150,
    val medium: Int = 250,
    val slow: Int = 400,

    /** Enters: decelerate hard so the element arrives settled rather than coasting. */
    val enter: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f),

    /** Exits: accelerate away — the eye does not need to track something that is leaving. */
    val exit: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f),

    /** Everything else, including colour and size changes on an element that stays put. */
    val standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f),

    /** The scale a pressed control shrinks to while a finger is on it. */
    val pressScale: Float = 0.96f,

    /**
     * The scale it passes through on the way back.
     *
     * The reason a tap on a well-built app feels answered. A press-and-release can be over in
     * forty milliseconds — far too fast for the shrink to finish — so without an overshoot on the
     * release the control barely moves and the tap reads as unregistered. `1f` disables it.
     */
    val pressOvershoot: Float = 1.02f,

    val pressDamping: Float = 0.6f,
    val pressStiffness: Float = 1400f,

    /** Sheets and drawers. Below 1 the sheet settles into its detent rather than stopping dead. */
    val sheetDamping: Float = 0.86f,

    /** Screen-to-screen. A long travel with any bounce in it reads as unstable. */
    val navigationDamping: Float = 1f,
) {
    /** Press down, and the settle back afterwards. */
    fun press(): FiniteAnimationSpec<Float> =
        spring(dampingRatio = pressDamping, stiffness = pressStiffness)

    /** The overshoot on release. Stiffer than the press, so the pop is quick rather than loose. */
    fun pressPop(): FiniteAnimationSpec<Float> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = pressStiffness * 1.6f)

    /** Screen-to-screen. Low stiffness so a long travel still feels weighted rather than snappy. */
    fun <T> navigation(): FiniteAnimationSpec<T> =
        spring(dampingRatio = navigationDamping, stiffness = 380f)

    /** Sheets and drawers. */
    fun <T> sheet(): FiniteAnimationSpec<T> =
        spring(dampingRatio = sheetDamping, stiffness = 460f)

    /** A value that follows a gesture one-to-one — a slider thumb, a swipe offset. */
    fun <T> tracking(): FiniteAnimationSpec<T> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 900f)

    fun <T> fadeIn(): FiniteAnimationSpec<T> = tween(medium, easing = enter)

    fun <T> fadeOut(): FiniteAnimationSpec<T> = tween(quick, easing = exit)

    /**
     * How far the screen underneath travels during a push, as a fraction of the one on top.
     *
     * Moving both the same distance reads as two unrelated slides. Moving the one underneath
     * *less* is what places it behind, and is the entire perception of depth in a stack.
     */
    val outgoingParallax: Float = 0.25f

    /** How much the outgoing screen dims. Enough to recede, not enough to look switched off. */
    val outgoingDim: Float = 0.28f

    /** The scale a screen settles back to when a sheet covers it. */
    val behindSheetScale: Float = 0.96f
}

internal val LocalAppMotion = staticCompositionLocalOf { AppMotion() }
