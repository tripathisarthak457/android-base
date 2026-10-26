package com.example.finalafter.crui.core.designsystem.foundation.adaptive

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Rect

/**
 * Whether a foldable is half open, and where the hinge that separates its halves is, in window
 * pixels. [hinge] is set only for a hinge that separates content: a flat fold under a continuous
 * screen is not a gap to lay out around.
 */
@Immutable
data class WindowPosture(val kind: Kind = Kind.Flat, val hinge: Rect? = null) {
    enum class Kind {
        /** Fully open or closed, or not a foldable. */
        Flat,

        /** Half open with a horizontal fold, lying like a laptop: content above, controls below. */
        Tabletop,

        /** Half open with a vertical fold, held like a book: one pane on each side. */
        Book,
    }
}
