package com.example.cmp.core.designsystem.foundation.layout

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Where a component that moves its host's content says so. The host counts it as overlay motion:
 * its Back gate counts presses meanwhile, and its navigation guard makes a navigation wait for the
 * motion to finish, never drops it. Each navigation host provides its own; outside one, reports go
 * nowhere.
 */
@ShellSeamApi
public fun interface HostMotion {
    /** [mover] starts ([moving] true) or stops moving the host's content. Every start is followed by a stop. */
    public fun report(mover: Any, moving: Boolean)
}

@ShellSeamApi
public val LocalHostMotion: ProvidableCompositionLocal<HostMotion> =
    staticCompositionLocalOf { HostMotion { _, _ -> } }
