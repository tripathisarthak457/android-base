package com.base.app.core.designsystem.foundation.glass

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

/**
 * How much liquid glass the device can afford, from the full effect down to none.
 *
 * Only floating chrome (the tab bar, its accessory and its trailing capsule) ever samples what is
 * behind it. Cards, chips, fields, sheets, dialogs and top-bar buttons are drawn glass in every
 * tier but [Solid], which is never more expensive than an ordinary card.
 *
 * | Tier | When | Chrome |
 * |---|---|---|
 * | [Live] | API 33+, a healthy device | blur, vibrancy, and a lens while dragged or moving to a tab |
 * | [LiveNoLens] | API 31–32, or a device warming up | blur and vibrancy |
 * | [Frosted] | API < 31, low RAM, battery saver, heat, a GPU behind | drawn, samples nothing |
 * | [Solid] | the person asked to reduce transparency | the style's solid bar |
 *
 * Read it from [LocalGlassQuality]. The shell provides [rememberGlassQuality]'s answer; a tier
 * switcher (the catalog, a benchmark) provides a fixed one.
 */
enum class GlassQuality {
    Live,
    LiveNoLens,
    Frosted,
    Solid,
    ;

    /** Chrome blurs the recorded scene, so a [GlassStage] records one. */
    val samplesScene: Boolean
        get() = this == Live || this == LiveNoLens

    /** A selection under a finger, or on its way to another tab, may refract the scene. */
    val refracts: Boolean
        get() = this == Live
}

/**
 * The tier glass draws at. Static: a tier changes a handful of times in a session at most, and
 * everything that reads it restyles when it does.
 */
val LocalGlassQuality = staticCompositionLocalOf { GlassQuality.Frosted }

/**
 * The tier this window can afford, re-evaluated when one of its inputs changes.
 *
 * The person's own choices take effect both ways at once: battery saver, and Reduce transparency
 * ([reduceTransparency], where `null` follows the system's high-contrast text on API 36 and
 * later). The measured signals only ever lower the tier for the rest of the session, so a device
 * hovering at a thermal threshold does not flip its chrome back and forth: the hottest thermal
 * status seen so far counts, and a GPU that fell behind the display's refresh rate in two
 * consecutive windows of frames while the scene was sampled leaves the app frosted, on this
 * system build and this install, until either changes.
 *
 * The first composition reads one small preferences file, where that verdict is kept, and the
 * app's install time.
 */
@Composable
fun rememberGlassQuality(reduceTransparency: Boolean? = null): GlassQuality {
    val context = LocalContext.current
    val view = LocalView.current
    val signals = remember(context) { DeviceGlassSignals(context.applicationContext) }
    DisposableEffect(signals) {
        signals.start()
        onDispose { signals.stop() }
    }
    val store = remember(context) { GlassVerdictStore.from(context.applicationContext) }
    var gpuKeepsUp by rememberSaveable { mutableStateOf(store.gpuKeepsUp) }
    var hottest by rememberSaveable { mutableIntStateOf(signals.thermalStatus) }
    val thermalStatus = maxOf(hottest, signals.thermalStatus)
    SideEffect { hottest = thermalStatus }

    val quality = glassQuality(
        GlassInputs(
            sdkInt = Build.VERSION.SDK_INT,
            lowRamDevice = signals.lowRamDevice,
            powerSave = signals.powerSave,
            thermalStatus = thermalStatus,
            gpuKeepsUp = gpuKeepsUp,
            reduceTransparency = reduceTransparency ?: signals.highContrastText,
        ),
    )

    // Only frames that sample the scene say anything about what glass costs this GPU.
    val watching = quality.samplesScene && gpuKeepsUp
    DisposableEffect(view, watching) {
        val stopWatching = if (watching) {
            watchGpuBudget(view) {
                gpuKeepsUp = false
                store.recordGpuFallsBehind()
            }
        } else {
            null
        }
        onDispose { stopWatching?.invoke() }
    }
    return quality
}
