package com.example.lean.core.designsystem.foundation.interaction

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView

/**
 * The moments that earn a haptic. An ordinary tap on a button, a row or a card is not one of them:
 * iOS plays nothing there, and a phone that buzzes on every touch soon feels cheap.
 */
enum class AppHaptic {
    /** A segment, tab, picker wheel or slider step changed. */
    Selection,

    /** A switch or checkbox turned on. */
    ToggleOn,

    /** A switch or checkbox turned off. */
    ToggleOff,

    /** A pull, swipe or drag crossed the point where letting go does something; a sheet snapped to a detent. */
    Threshold,

    /** Something finished and succeeded. */
    Confirm,

    /** Something was refused. */
    Reject,

    /** A long press opened a menu. */
    LongPress,
}

/** Plays haptics, or nothing when the app turned them off. The system's own setting applies on top. */
@Immutable
class AppHaptics internal constructor(
    private val view: View?,
    private val enabled: Boolean,
) {
    fun perform(haptic: AppHaptic) {
        if (enabled) view?.performHapticFeedback(haptic.constant())
    }
}

internal fun AppHaptic.constant(): Int {
    val sdk = Build.VERSION.SDK_INT
    return when (this) {
        AppHaptic.Selection ->
            if (sdk >= 34) HapticFeedbackConstants.SEGMENT_TICK else HapticFeedbackConstants.CLOCK_TICK
        AppHaptic.ToggleOn ->
            if (sdk >= 34) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.CONTEXT_CLICK
        AppHaptic.ToggleOff ->
            if (sdk >= 34) HapticFeedbackConstants.TOGGLE_OFF else HapticFeedbackConstants.CONTEXT_CLICK
        AppHaptic.Threshold ->
            if (sdk >= 34) HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE else HapticFeedbackConstants.CONTEXT_CLICK
        AppHaptic.Confirm ->
            if (sdk >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY
        AppHaptic.Reject ->
            if (sdk >= 30) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS
        AppHaptic.LongPress -> HapticFeedbackConstants.LONG_PRESS
    }
}

/** Whether the app plays haptics at all. Set once, by `AppTheme(hapticsEnabled = …)`. */
val LocalHapticsEnabled = staticCompositionLocalOf { true }

/** The haptics for the current theme. Cheap; read it wherever a control needs to speak. */
@Composable
fun rememberAppHaptics(): AppHaptics {
    val view = LocalView.current
    val enabled = LocalHapticsEnabled.current
    return remember(view, enabled) { AppHaptics(view, enabled) }
}
