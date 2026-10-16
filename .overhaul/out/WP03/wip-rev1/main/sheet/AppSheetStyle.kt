package com.base.app.core.designsystem.component.sheet

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.Dp
import com.base.app.core.designsystem.theme.SheetMaterial

/** A sheet's colours. */
@Immutable
class AppSheetColors(
    /** The body. Frosted and Glass draw it translucent at partial detents. */
    val container: Color,
    val content: Color,
    val grabber: Color,
    /** A hairline around the body; transparent for none. */
    val rim: Color,
    /** The dim at full strength, alpha included. */
    val scrim: Color,
) {
    fun copy(
        container: Color = this.container,
        content: Color = this.content,
        grabber: Color = this.grabber,
        rim: Color = this.rim,
        scrim: Color = this.scrim,
    ): AppSheetColors = AppSheetColors(container, content, grabber, rim, scrim)

    override fun equals(other: Any?): Boolean = other is AppSheetColors &&
        container == other.container && content == other.content && grabber == other.grabber &&
        rim == other.rim && scrim == other.scrim

    override fun hashCode(): Int {
        var result = container.hashCode()
        result = 31 * result + content.hashCode()
        result = 31 * result + grabber.hashCode()
        result = 31 * result + rim.hashCode()
        return 31 * result + scrim.hashCode()
    }
}

/**
 * How a sheet looks: its material, colours, corner, the inset it floats at, its widest and its
 * shadow. Build one with [AppSheetDefaults.style] and change a field with [copy]. Nothing here
 * changes what the sheet does.
 */
@Immutable
class AppSheetStyle internal constructor(
    val material: SheetMaterial,
    val colors: AppSheetColors,
    /** All four corners, floating or attached. */
    val cornerRadius: Dp,
    /** The gap to the side and bottom edges at partial detents; 0 keeps the sheet attached. */
    val floatingInset: Dp,
    val maxWidth: Dp,
    /**
     * Drawn outside the shape, and only where it separates the sheet from what is beside it: a
     * non-modal sheet, a side panel, a form sheet or an anchored card, in light theme. Null for none.
     */
    val shadow: Shadow?,
) {
    fun copy(
        material: SheetMaterial = this.material,
        colors: AppSheetColors = this.colors,
        cornerRadius: Dp = this.cornerRadius,
        floatingInset: Dp = this.floatingInset,
        maxWidth: Dp = this.maxWidth,
        shadow: Shadow? = this.shadow,
    ): AppSheetStyle = AppSheetStyle(material, colors, cornerRadius, floatingInset, maxWidth, shadow)

    override fun equals(other: Any?): Boolean = other is AppSheetStyle &&
        material == other.material && colors == other.colors && cornerRadius == other.cornerRadius &&
        floatingInset == other.floatingInset && maxWidth == other.maxWidth && shadow == other.shadow

    override fun hashCode(): Int {
        var result = material.hashCode()
        result = 31 * result + colors.hashCode()
        result = 31 * result + cornerRadius.hashCode()
        result = 31 * result + floatingInset.hashCode()
        result = 31 * result + maxWidth.hashCode()
        return 31 * result + shadow.hashCode()
    }
}
