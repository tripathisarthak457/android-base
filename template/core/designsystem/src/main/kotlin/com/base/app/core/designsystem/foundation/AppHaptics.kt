package com.base.app.core.designsystem.foundation

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView

/**
 * What a control has just done, in the language a hand understands.
 *
 * Named by meaning rather than by waveform, because the waveform is the platform's business: an
 * OEM with a good linear actuator plays a crisp click for [Select] and one with a rotating-mass
 * motor plays the closest thing it has, and neither is something an app should be choosing.
 */
enum class HapticEffect {
    /** A button, a row, a menu item. The lightest thing there is. */
    Tap,

    /** The value changed to a different one: a segment, a tab, a chip, a wheel item. */
    Select,

    /** A switch or a checkbox went on or off. Distinct from [Select] on devices that can. */
    Toggle,

    /** One step of a continuous control passing a detent: a slider notch, a stepper. */
    Tick,

    /** Something finished and succeeded — a form submitted, a swipe committed. */
    Confirm,

    /** Something was refused: a rejected input, a swipe that sprang back. */
    Reject,

    /** A long press was recognised. */
    LongPress,

    /** A drag or a pull crossed the threshold where letting go would do something. */
    Threshold,
}

/**
 * Plays haptics, or does nothing.
 *
 * ## Why not `Vibrator`
 *
 * `View.performHapticFeedback` needs no permission, is routed through the device's own haptics
 * tuning, and — the part that matters — is silently dropped when the user has turned haptic
 * feedback off in system settings. Driving the `Vibrator` directly ignores that setting, which
 * makes an app the one that buzzes after somebody has explicitly asked every app to stop.
 *
 * ## Why the constants are chosen per API level
 *
 * The expressive constants arrived late — `CONFIRM` and `REJECT` in API 30, `TOGGLE_ON` and
 * `SEGMENT_TICK` in API 34. Each falls back to the nearest older constant rather than to nothing,
 * so an older device still feels a distinction between selecting and confirming, just a coarser
 * one.
 */
@Immutable
class AppHaptics internal constructor(
    private val view: View?,
    private val enabled: Boolean,
) {
    fun perform(effect: HapticEffect) {
        if (!enabled) return
        view?.performHapticFeedback(effect.constant())
    }

    /**
     * One table per API era, rather than one table with a version check in every row.
     *
     * The expressive constants arrived in two waves — `CONFIRM` and `REJECT` in API 30,
     * `TOGGLE_ON` and the segment ticks in API 34 — and writing it this way keeps each era
     * readable as a whole, keeps every constant behind a real guard for lint, and keeps any one
     * function simple enough to take in at a glance.
     */
    private fun HapticEffect.constant(): Int = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> modern()
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> expressive()
        else -> legacy()
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun HapticEffect.modern(): Int = when (this) {
        HapticEffect.Tap -> HapticFeedbackConstants.KEYBOARD_TAP
        HapticEffect.Select -> HapticFeedbackConstants.SEGMENT_TICK
        HapticEffect.Toggle -> HapticFeedbackConstants.TOGGLE_ON
        // The frequent variant exists precisely for a value that ticks many times in one gesture;
        // the ordinary tick played thirty times during a fling is a buzz, not feedback.
        HapticEffect.Tick -> HapticFeedbackConstants.SEGMENT_FREQUENT_TICK
        HapticEffect.Confirm -> HapticFeedbackConstants.CONFIRM
        HapticEffect.Reject -> HapticFeedbackConstants.REJECT
        HapticEffect.LongPress -> HapticFeedbackConstants.LONG_PRESS
        HapticEffect.Threshold -> HapticFeedbackConstants.GESTURE_START
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun HapticEffect.expressive(): Int = when (this) {
        HapticEffect.Tap -> HapticFeedbackConstants.KEYBOARD_TAP
        HapticEffect.Select, HapticEffect.Tick -> HapticFeedbackConstants.CLOCK_TICK
        HapticEffect.Toggle -> HapticFeedbackConstants.KEYBOARD_TAP
        HapticEffect.Confirm -> HapticFeedbackConstants.CONFIRM
        HapticEffect.Reject -> HapticFeedbackConstants.REJECT
        HapticEffect.LongPress -> HapticFeedbackConstants.LONG_PRESS
        HapticEffect.Threshold -> HapticFeedbackConstants.GESTURE_START
    }

    private fun HapticEffect.legacy(): Int = when (this) {
        HapticEffect.Tap, HapticEffect.Toggle -> HapticFeedbackConstants.KEYBOARD_TAP
        HapticEffect.Select, HapticEffect.Tick, HapticEffect.Threshold ->
            HapticFeedbackConstants.CLOCK_TICK
        HapticEffect.Confirm, HapticEffect.Reject, HapticEffect.LongPress ->
            HapticFeedbackConstants.LONG_PRESS
    }
}

/**
 * Whether the app plays haptics at all.
 *
 * Set once, at [com.base.app.core.designsystem.theme.AppTheme]:
 *
 * ```
 * AppTheme(hapticsEnabled = settings.hapticsEnabled) { … }
 * ```
 *
 * Wire it to a preference and the whole app goes quiet — the components read it through
 * [rememberAppHaptics] and none of them own the decision. The system setting still applies on top
 * of this; turning it on here cannot override a user who has turned haptics off on their phone.
 */
val LocalAppHapticsEnabled = staticCompositionLocalOf { true }

@Composable
@ReadOnlyComposable
private fun currentHapticView(): View? = LocalView.current

/** The haptics for the current theme. Cheap; read it wherever a control needs to speak. */
@Composable
fun rememberAppHaptics(): AppHaptics {
    val view = currentHapticView()
    val enabled = LocalAppHapticsEnabled.current
    return remember(view, enabled) { AppHaptics(view, enabled) }
}
