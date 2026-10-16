package com.base.app.core.designsystem.foundation.layout

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Rect

/**
 * The window-space bounds of the pane this content is in, or null for the whole window. A non-modal
 * sheet stays inside them. The holder never changes; [rect] is read in measure.
 */
@Stable
class PaneBounds(private val bounds: () -> Rect?) {
    val rect: Rect? get() = bounds()
}

val LocalPaneBounds: ProvidableCompositionLocal<PaneBounds> = staticCompositionLocalOf { WholeWindow }

private val WholeWindow = PaneBounds { null }
