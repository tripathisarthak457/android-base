package com.example.videoalone.core.designsystem.component.sheet

import androidx.compose.foundation.gestures.DraggableAnchors
import com.example.videoalone.core.designsystem.component.sheet.layout.nominalOffset

/** Where a sheet can be: hidden, or resting at one of its detents. */
internal sealed interface SheetStop {
    data object Hidden : SheetStop

    data class At(val detent: SheetDetent) : SheetStop
}

/** The stop a request for [detent] lands on: the detent, or the active stop nearest it. */
internal fun AppSheetState.activeFor(detent: SheetDetent): SheetDetent {
    val stops = stops ?: return detent
    if (stops.single) return stops.detents[0]
    if (stops.has(detent)) return detent
    val inputs = inputs ?: return stops.detents[0]
    val wouldBe = nominalOffset(detent, inputs) ?: return stops.detents[0]
    return stops.detents[stops.nearestIndex(wouldBe)]
}

/** Writes the anchors for the current stops; [keep] is where an idle sheet stays. */
internal fun AppSheetState.syncAnchors(keep: SheetStop) {
    val stops = stops ?: return
    val withHidden = !shown || leaving || presenting || host?.dismissAllowed != false ||
        anchored.settledValue == SheetStop.Hidden
    val anchors = DraggableAnchors {
        if (withHidden) SheetStop.Hidden at stops.hidden
        for (index in 0 until stops.count) SheetStop.At(stops.detents[index]) at stops.offsets[index]
    }
    val kept = if (keep is SheetStop.At && !stops.has(keep.detent)) SheetStop.At(activeFor(keep.detent)) else keep
    anchored.updateAnchors(anchors, if (kept == SheetStop.Hidden && !withHidden) SheetStop.At(stops.detents[0]) else kept)
}
