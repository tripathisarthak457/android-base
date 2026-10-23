package com.base.app.core.navigation

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Whether the screens of the host a screen is in have stopped moving: false from a push or a pop
 * until its slide has all but arrived, and while the back card commits; always true outside a host.
 *
 * A screen's first composition should be its final one. Content that arrives later (related rows, a
 * second section) waits for this, so it is not composed in the middle of the slide:
 *
 * ```
 * val settled = LocalNavigationSettled.current
 * LaunchedEffect(Unit) {
 *     snapshotFlow { settled() }.first { it }
 *     onEvent(DetailEvent.LoadRelated)
 * }
 * ```
 *
 * Read it in an effect, never in composition: it changes twice per transition.
 */
val LocalNavigationSettled: ProvidableCompositionLocal<() -> Boolean> = staticCompositionLocalOf { AlwaysSettled }

private val AlwaysSettled: () -> Boolean = { true }
