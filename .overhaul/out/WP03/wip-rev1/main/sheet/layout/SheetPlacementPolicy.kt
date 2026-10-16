package com.base.app.core.designsystem.component.sheet.layout

import com.base.app.core.designsystem.component.sheet.SheetPlacement
import com.base.app.core.designsystem.foundation.adaptive.SizeClass
import com.base.app.core.designsystem.foundation.adaptive.WindowHeight
import com.base.app.core.designsystem.foundation.adaptive.WindowPosture
import com.base.app.core.designsystem.foundation.adaptive.WindowWidth

/**
 * Where a sheet sits in this window (§8 of the sheet specification). A modal sheet decides from the
 * window's class and a non-modal one from its pane's, so a modal sheet opened from a narrow pane on
 * a tablet is still a form sheet. Explicit Bottom and Center are always honoured; Start and End
 * need an expanded width and fall back to the bottom below it.
 */
internal fun resolvePlacement(
    requested: SheetPlacement,
    modal: Boolean,
    sizeClass: SizeClass,
    posture: WindowPosture.Kind,
): SheetPlacement {
    val wide = sizeClass.width >= WindowWidth.Expanded
    return when (requested) {
        SheetPlacement.Bottom, SheetPlacement.Center -> requested
        SheetPlacement.Start, SheetPlacement.End -> if (wide) requested else SheetPlacement.Bottom
        SheetPlacement.Automatic -> when {
            sizeClass.height == WindowHeight.Compact -> SheetPlacement.Bottom
            // A sheet across a tabletop crease would be split by it: it rises from the bottom half.
            posture == WindowPosture.Kind.Tabletop -> SheetPlacement.Bottom
            modal && sizeClass.width >= WindowWidth.Medium -> SheetPlacement.Center
            !modal && wide -> SheetPlacement.Start
            else -> SheetPlacement.Bottom
        }
    }
}
