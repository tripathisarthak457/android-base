package com.example.cmp.core.designsystem.foundation.layout

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Rect

/**
 * The window-space bounds of the pane this content is in; null for the whole window. A non-modal
 * sheet stays inside them, and while it blocks, it blocks only this pane.
 */
@ShellSeamApi
@Stable
public class PaneBounds(
    private val bounds: () -> Rect?,
    private val onBlock: (blocker: Any, blocked: Boolean) -> Unit = { _, _ -> },
) {
    /** The stored rect, replaced only when the pane moves: reading it allocates nothing. Read in measure or placement. */
    public val rect: Rect? get() = bounds()

    /**
     * A sheet bounded by this pane blocks it: the pane's subtree is hidden from accessibility while
     * any blocker is registered.
     */
    public fun block(blocker: Any, blocked: Boolean): Unit = onBlock(blocker, blocked)
}

@ShellSeamApi
public val LocalPaneBounds: ProvidableCompositionLocal<PaneBounds> = staticCompositionLocalOf { PaneBounds({ null }) }
