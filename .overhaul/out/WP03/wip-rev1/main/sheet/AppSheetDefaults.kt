package com.base.app.core.designsystem.component.sheet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.component.sheet.chrome.AppSheetGrabber
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.SheetMaterial
import com.base.app.core.designsystem.theme.tokens.DesignTokens

/** The values an [AppSheet] starts from. Every number is a design token shared with the other platforms. */
object AppSheetDefaults {
    /** iOS's default: a sheet rests at the large detent only. */
    val Detents: List<SheetDetent> = listOf(SheetDetent.Large)

    /** Where [SheetDetent.Medium] puts the sheet's top, as a share of its bounds' height. */
    const val MediumFraction: Float = DesignTokens.Sheet.Detent.mediumFraction

    /** The gap between the safe area's top and [SheetDetent.Large]'s top. */
    val LargeTopGap: Dp = DesignTokens.Sheet.Detent.largeTopGap

    val GrabberSize: DpSize = DpSize(DesignTokens.Sheet.Grabber.width, DesignTokens.Sheet.Grabber.height)

    /** The grabber's distance from the sheet's top edge. */
    val GrabberTop: Dp = DesignTokens.Sheet.Grabber.top

    /** Where content starts when there is no header: clear of the grabber. */
    val ContentTop: Dp = DesignTokens.Sheet.Grabber.contentTop

    /** Where a header starts, below the grabber. */
    val HeaderTop: Dp = DesignTokens.Sheet.Header.top

    val HeaderMinHeight: Dp = DesignTokens.Sheet.Header.minHeight

    /** A form sheet is at most this share of its window's height, and at most [FormMaxHeight]. */
    const val FormHeightFraction: Float = DesignTokens.Sheet.Form.heightFraction

    val FormMaxHeight: Dp = DesignTokens.Sheet.Form.maxHeight

    /** The gap around a panel placed at the start or end edge. */
    val SideInset: Dp = DesignTokens.Sheet.Side.inset

    const val GrabberAlpha: Float = DesignTokens.Sheet.Grabber.alpha

    /** The default grabber: shown when the sheet has more than one place to rest. */
    val Grabber: @Composable AppSheetScope.() -> Unit = { AppSheetGrabber() }

    @Composable
    @ReadOnlyComposable
    fun colors(
        container: Color = AppTheme.colors.surfaceElevated,
        content: Color = AppTheme.colors.contentPrimary,
        grabber: Color = AppTheme.colors.contentTertiary.copy(alpha = GrabberAlpha),
        rim: Color = solidRim(),
        scrim: Color = AppTheme.colors.scrim,
    ): AppSheetColors = AppSheetColors(container, content, grabber, rim, scrim)

    /** For inset-grouped content (`AppSection`): the grouped background behind grouped rows, as iOS's grouped sheets. */
    @Composable
    @ReadOnlyComposable
    fun groupedColors(): AppSheetColors = colors(container = AppTheme.colors.backgroundGrouped)

    /**
     * A hairline around the body in dark theme, where a dark sheet on a dark dim would lose its
     * edge; transparent in light theme, where the dim or the shadow separates it.
     */
    @Composable
    @ReadOnlyComposable
    fun solidRim(): Color =
        if (AppTheme.colors.isLight) Color.Transparent else Color.White.copy(alpha = DesignTokens.Sheet.Rim.solidDarkAlpha)

    /** Soft, in light theme only: in dark theme a shadow would vanish into the dim and the rim shows the edge. */
    @Composable
    @ReadOnlyComposable
    fun shadow(): Shadow? = if (!AppTheme.colors.isLight) {
        null
    } else {
        Shadow(
            radius = DesignTokens.Sheet.Shadow.blur,
            color = Color.Black,
            offset = DpOffset(0.dp, DesignTokens.Sheet.Shadow.y),
            alpha = DesignTokens.Sheet.Shadow.alpha,
        )
    }

    @Composable
    @ReadOnlyComposable
    fun style(
        material: SheetMaterial = SheetMaterial.Automatic,
        colors: AppSheetColors = colors(),
        cornerRadius: Dp = AppTheme.style.sheetRadius,
        floatingInset: Dp = AppTheme.style.sheetInset,
        maxWidth: Dp = AppTheme.layout.sheetMaxWidth,
        shadow: Shadow? = shadow(),
    ): AppSheetStyle = AppSheetStyle(material, colors, cornerRadius, floatingInset, maxWidth, shadow)
}
