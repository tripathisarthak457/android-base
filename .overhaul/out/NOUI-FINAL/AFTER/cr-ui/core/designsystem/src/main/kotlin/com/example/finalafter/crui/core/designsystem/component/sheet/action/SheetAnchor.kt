package com.example.finalafter.crui.core.designsystem.component.sheet.action

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onPlaced

/**
 * The bounds an anchored card points at, recorded in placement without snapshot writes: a card
 * opened from it reads them when it is laid out, and nothing recomposes as the anchor moves.
 */
@Stable
class SheetAnchor internal constructor() {
    internal var bounds: Rect? = null
    internal val focus = FocusRequester()
}

@Composable
fun rememberSheetAnchor(): SheetAnchor = remember { SheetAnchor() }

/** Records the bounds and takes focus back when the anchored card closes. */
fun Modifier.sheetAnchor(anchor: SheetAnchor): Modifier = this
    .onPlaced { anchor.bounds = it.boundsInWindow() }
    .focusRequester(anchor.focus)
