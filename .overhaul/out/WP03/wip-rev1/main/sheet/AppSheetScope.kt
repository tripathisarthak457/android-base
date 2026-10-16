package com.base.app.core.designsystem.component.sheet

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.annotation.FrequentlyChangingValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.base.app.core.designsystem.theme.SheetMaterial

/** What a sheet's slots and content can reach. */
@Stable
interface AppSheetScope {
    val sheetState: AppSheetState

    /**
     * Padding for the content's scroller: the footer, the navigation bar or the keyboard, and with
     * [SheetContentInteraction.Scrolls] the part of the content below the visible bottom at the
     * settled detent, each counted once. It is read in measure: a change re-measures the scroller
     * and never recomposes it. It changes only when the sheet settles, the footer's height changes
     * or the keyboard's target changes; never during a drag.
     */
    val contentPadding: PaddingValues

    /** How far the content has scrolled under the header, 0 to 1 over the first 8 dp. Read in draw. */
    @get:FrequentlyChangingValue
    val contentScrolled: Float

    /** True while another sheet is stacked on this one. */
    val isCovered: Boolean

    /** Closes the sheet as the dim would: a [SheetDismissGuard] or `dismissible = false` turns it into an attempt. */
    fun requestDismiss()

    /** Closes the sheet without asking, as a Done button does after the work is saved. Calls `onDismissRequest`. */
    fun dismiss()
}

/**
 * The sheet a composable is drawn in, or null outside one. `AppScaffold` lets the sheet's material
 * show instead of painting its own background; `AppTopBar` pads no status bar and offers a close
 * button where it would offer Back at the root; `AppBottomActionBar` paints no background and pads
 * no inset.
 */
val LocalSheetSurface: ProvidableCompositionLocal<SheetSurface?> = staticCompositionLocalOf { null }

/** The sheet around some content: what it is made of, its body's colour and its scope. */
@Stable
class SheetSurface internal constructor(
    val material: SheetMaterial,
    val container: Color,
    val scope: AppSheetScope,
)
