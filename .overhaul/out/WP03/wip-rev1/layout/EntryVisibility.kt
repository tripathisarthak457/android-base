package com.base.app.core.designsystem.foundation.layout

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Whether this navigation entry is uncovered: the top of its stack, with nothing presented over its
 * host. A sheet opened from the entry slides away while it is covered and comes back when it is
 * not. The holder never changes; read [isUncovered] in an effect or a draw block.
 */
@Stable
class EntryVisibility(private val uncovered: () -> Boolean) {
    val isUncovered: Boolean get() = uncovered()
}

val LocalEntryVisibility: ProvidableCompositionLocal<EntryVisibility> = staticCompositionLocalOf { AlwaysUncovered }

private val AlwaysUncovered = EntryVisibility { true }
