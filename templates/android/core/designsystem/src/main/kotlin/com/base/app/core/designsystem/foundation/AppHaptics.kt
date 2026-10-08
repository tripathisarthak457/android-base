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

/** What a control has just done, in the language a hand understands. */
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

/** Plays haptics, or does nothing. */
@Immutable
class AppHaptics internal constructor(
    private val view: View?,
    private val enabled: Boolean,
) {
    fun perform(effect: HapticEffect) {
        if (!enabled) return
        view?.performHapticFeedback(effect.constant())
    }

    /** One table per API era, rather than one table with a version check in every row. */
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
 * Whether the app plays haptics at all. Set once, at
 * [com.base.app.core.designsystem.theme.AppTheme]:
 *
 * ```
 * AppTheme(hapticsEnabled = settings.hapticsEnabled) { … }
 * ```
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
