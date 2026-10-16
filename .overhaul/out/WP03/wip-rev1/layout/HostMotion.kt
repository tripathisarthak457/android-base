package com.base.app.core.designsystem.foundation.layout

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Where a component that moves its host's content says so, so the host's Back gate counts presses
 * meanwhile instead of popping under a moving sheet. Each navigation host provides its own; outside
 * one, reports go nowhere.
 */
fun interface HostMotion {
    /** [mover] starts ([moving] true) or stops moving the host's content. Every start is followed by a stop. */
    fun report(mover: Any, moving: Boolean)
}

val LocalHostMotion: ProvidableCompositionLocal<HostMotion> = staticCompositionLocalOf { HostMotion { _, _ -> } }
