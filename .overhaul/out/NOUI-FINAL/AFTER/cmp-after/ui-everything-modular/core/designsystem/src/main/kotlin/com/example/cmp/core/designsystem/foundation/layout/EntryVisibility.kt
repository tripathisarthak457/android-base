package com.example.cmp.core.designsystem.foundation.layout

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.GraphicsLayerScope

/**
 * How the navigation entry this content is in is shown. Every getter reads snapshot state: read it
 * in placement, a layer block, draw or a `snapshotFlow`, never in composition, so a tab switch or a
 * push recomposes nothing that only follows it.
 *
 * A sheet opened from the entry parks while the entry's tab is not selected, slides away while the
 * entry is covered, and moves with [pose] while the entry itself moves.
 */
@ShellSeamApi
@Stable
public class EntryVisibility(
    private val tabSelected: () -> Boolean,
    private val uncovered: () -> Boolean,
    private val settled: () -> Boolean,
    /** The transform the entry's own layer has this frame. */
    public val pose: EntryPose = EntryPose.Identity,
) {
    /** The entry's tab is the selected one; true outside tabs. Flips on the switch frame. */
    public val isTabSelected: Boolean get() = tabSelected()

    /** The entry is the target top of its stack and nothing is presented above its host. */
    public val isUncovered: Boolean get() = uncovered()

    /** The entry's own arrival (a push, the pop that reveals it, a tab reveal) is 99 % done. */
    public val isSettled: Boolean get() = settled()
}

/** The entry layer's transform, for a layer drawn outside the entry that must move with it. */
@ShellSeamApi
public interface EntryPose {
    /**
     * Applies the entry's scale, translation, alpha and pivot to [scope], a layer whose top-left is at
     * [origin] in window pixels. Called from a layer block, once per frame of the entry's motion.
     */
    public fun applyTo(scope: GraphicsLayerScope, origin: Offset)

    public companion object {
        /** An entry that does not move. */
        public val Identity: EntryPose = object : EntryPose {
            override fun applyTo(scope: GraphicsLayerScope, origin: Offset) = Unit
        }
    }
}

@ShellSeamApi
public val LocalEntryVisibility: ProvidableCompositionLocal<EntryVisibility> =
    staticCompositionLocalOf { EntryVisibility({ true }, { true }, { true }) }
