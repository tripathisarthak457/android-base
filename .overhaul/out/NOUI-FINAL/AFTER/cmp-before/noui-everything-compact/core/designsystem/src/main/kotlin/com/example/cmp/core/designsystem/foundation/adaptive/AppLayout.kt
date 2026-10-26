package com.example.cmp.core.designsystem.foundation.adaptive

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Layout measurements that follow the size class of the pane they are read in. Components and
 * screens read these instead of choosing numbers, so one change here reflows the app on every form
 * factor. A width of [Dp.Infinity] means the content may take the whole pane.
 */
@Immutable
public data class AppLayout(
    /** The margin between content and the pane's edge. */
    val gutter: Dp,
    /** The list half of a list-detail layout. */
    val listPaneWidth: Dp,
    /** The side pane beside a screen's main content: details, filters, a document's outline. */
    val supportingPaneWidth: Dp,
    /** The navigation rail that replaces the tab bar on a medium window. */
    val railWidth: Dp,
    /** The sidebar that replaces the rail on a wide window. */
    val sidebarWidth: Dp,
    /** Prose and settings-style lists, about 70 characters to the line. */
    val readableMaxWidth: Dp,
    /** A single form: sign-in, a short edit screen. Wider than this and the fields read as lines. */
    val formMaxWidth: Dp,
    /** The widest a single column of mixed content gets before it is centred. */
    val contentMaxWidth: Dp,
    /** How wide a sheet, alert or toast gets before it stops stretching. */
    val sheetMaxWidth: Dp,
    /** A sheet presented as a centred card rather than from the bottom edge, on a medium window or wider. */
    val formSheetWidth: Dp,
    /** The narrowest a small tile (a photo, an icon with a label) gets in a grid. */
    val gridMinTileWidth: Dp,
    /** The narrowest a card gets in a grid; the column count follows from it. */
    val gridMinCardWidth: Dp,
) {
    public companion object {
        /** Material's pane widths and Apple's readable widths, per width class. */
        public fun forClass(sizeClass: SizeClass): AppLayout = when (sizeClass.width) {
            WindowWidth.Compact -> Base.copy(gutter = 16.dp, readableMaxWidth = Dp.Infinity, contentMaxWidth = Dp.Infinity)
            WindowWidth.Medium -> Base.copy(gutter = 24.dp, contentMaxWidth = Dp.Infinity)
            WindowWidth.Expanded -> Base
            WindowWidth.Large -> Base.copy(gutter = 32.dp, listPaneWidth = 412.dp, supportingPaneWidth = 412.dp, formSheetWidth = 600.dp)
            WindowWidth.ExtraLarge -> Base.copy(
                gutter = 32.dp,
                listPaneWidth = 412.dp,
                supportingPaneWidth = 412.dp,
                sidebarWidth = 304.dp,
                contentMaxWidth = 1200.dp,
                formSheetWidth = 600.dp,
            )
        }

        private val Base = AppLayout(
            gutter = 24.dp,
            listPaneWidth = 360.dp,
            supportingPaneWidth = 360.dp,
            railWidth = 88.dp,
            sidebarWidth = 280.dp,
            readableMaxWidth = 720.dp,
            formMaxWidth = 480.dp,
            contentMaxWidth = 1040.dp,
            sheetMaxWidth = 640.dp,
            formSheetWidth = 540.dp,
            gridMinTileWidth = 160.dp,
            gridMinCardWidth = 300.dp,
        )
    }
}
