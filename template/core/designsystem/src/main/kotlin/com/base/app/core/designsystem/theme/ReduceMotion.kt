package com.base.app.core.designsystem.theme

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Whether the user has turned animations off in system settings.
 *
 * Read from `TRANSITION_ANIMATION_SCALE`, which is the setting Android's own "Remove animations"
 * accessibility toggle and every developer-options animation switch write to. Honouring it is not
 * a nicety: for someone with a vestibular disorder a full-screen parallax slide is not a flourish,
 * it is nausea, and the setting is how they have already told every app to stop.
 *
 * Read once and remembered — the value cannot change without the process being recreated, and
 * a `Settings` lookup on every recomposition of every animated component would be a real cost.
 */
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.TRANSITION_ANIMATION_SCALE,
                1f,
            ) == 0f
        }.getOrDefault(false)
    }
}
