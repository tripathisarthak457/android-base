package com.example.videoalone.core.designsystem.component.sheet

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.annotation.FrequentlyChangingValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.videoalone.core.designsystem.theme.SheetMaterial

/** What a sheet's slots and content can reach. */
@Stable
interface AppSheetScope {
    val sheetState: AppSheetState

    /**
     * Padding for the content's scroller. Start and end: the sheet's gutter (its placement class's
     * AppLayout.gutter), so text never meets the rounded clip while the sheet floats; full-bleed rows
     * ignore it and pad their own text. Bottom: the footer, the navigation bar or the keyboard, and
     * with Scrolls the part of the content below the visible bottom at the settled detent, each
     * counted once. Top: 0. Backed by snapshot state and read in measure: a change re-measures the
     * scroller and never recomposes it. It changes only when the sheet settles, the footer's height
     * changes, the keyboard's target changes or the placement class changes; never during a drag.
     */
    val contentPadding: PaddingValues

    /** How far the content has scrolled under the header, 0 to 1 over sheet.header.scrollEdgeRamp. Read in draw. */
    @get:FrequentlyChangingValue
    val contentScrolled: Float

    /** True while another sheet is stacked on this one. */
    val isCovered: Boolean

    /** Closes the sheet as the scrim would: a SheetDismissGuard or dismissible = false turns it into an attempt. */
    fun requestDismiss()

    /** Closes the sheet without asking: a Done button after the work is saved. Calls onDismissRequest. */
    fun dismiss()
}

internal val LocalSheetSurfaceProvider = staticCompositionLocalOf<SheetSurface?> { null }

/**
 * The sheet a composable is drawn in, or null outside one; only the sheet provides it. AppScaffold
 * lets the sheet's material show instead of painting its own background; AppTopBar pads no status
 * bar and shows a close button where it would show Back at the root; AppBottomActionBar paints the
 * sheet's colour, and the insets it pads are already consumed by the sheet.
 */
val LocalSheetSurface: CompositionLocal<SheetSurface?> = LocalSheetSurfaceProvider

/** One per sheet, remembered: the declared material and container, never what animates. */
@Stable
class SheetSurface internal constructor(val material: SheetMaterial, val container: Color, val scope: AppSheetScope)

/** The scope a header or footer provider draws in when it is not inside a sheet: it draws in place. */
internal object OutsideSheetScope : AppSheetScope {
    override val sheetState: AppSheetState = AppSheetState()
    override val contentPadding: PaddingValues = PaddingValues(0.dp)
    override val contentScrolled: Float get() = 0f
    override val isCovered: Boolean get() = false

    override fun requestDismiss() = Unit

    override fun dismiss() = Unit
}
