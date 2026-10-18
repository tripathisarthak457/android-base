package com.example.wp04.everything.core.designsystem.component.sheet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.example.wp04.everything.core.designsystem.component.sheet.chrome.AppSheetGrabber
import com.example.wp04.everything.core.designsystem.theme.AppTheme
import com.example.wp04.everything.core.designsystem.theme.SheetMaterial
import com.example.wp04.everything.core.designsystem.theme.tokens.DesignTokens

/** The defaults of [AppSheet]'s parameters. The sheet's geometry is the design tokens', not API. */
object AppSheetDefaults {
    /** iOS's default: a sheet rests at the large detent only. */
    val Detents: List<SheetDetent> = listOf(SheetDetent.Large)

    /** The grabber pill, shown while the sheet has more than one place to rest. */
    val Grabber: @Composable AppSheetScope.() -> Unit = { AppSheetGrabber() }

    @Composable
    @ReadOnlyComposable
    fun colors(
        container: Color = AppTheme.colors.surfaceElevated,
        content: Color = AppTheme.colors.contentPrimary,
        grabber: Color = AppTheme.colors.contentTertiary.copy(alpha = DesignTokens.Sheet.Grabber.alpha),
        rim: Color = solidRim(),
        scrim: Color = AppTheme.colors.scrim,
    ): AppSheetColors = AppSheetColors(container, content, grabber, rim, scrim)

    /** For inset-grouped content (AppSection): the grouped background, as iOS's grouped sheets. */
    @Composable
    @ReadOnlyComposable
    fun groupedColors(): AppSheetColors = colors(container = AppTheme.colors.backgroundGrouped)

    /**
     * A hairline in dark theme, where a dark sheet on a dark dim would lose its edge; transparent in
     * light theme, where the dim or the shadow separates it.
     */
    @Composable
    @ReadOnlyComposable
    fun solidRim(): Color =
        if (AppTheme.colors.isLight) Color.Transparent else Color.White.copy(alpha = DesignTokens.Sheet.Rim.solidDarkAlpha)

    /** Soft in light theme; null in dark, where the rim shows the edge. */
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

    /**
     * The look of a sheet. [shadow] is drawn only where it separates the sheet from what is beside
     * it: a colour with an alpha, never a brush or another blend mode, which the nine-slice it
     * is drawn with cannot reproduce. [maxWidth] caps the placement's own width.
     */
    @Composable
    @ReadOnlyComposable
    fun style(
        material: SheetMaterial = SheetMaterial.Automatic,
        colors: AppSheetColors = colors(),
        containerBrush: Brush? = null,
        cornerRadius: Dp = AppTheme.style.sheetRadius,
        floatingInset: Dp = AppTheme.style.sheetInset,
        maxWidth: Dp = Dp.Unspecified,
        shadow: Shadow? = shadow(),
    ): AppSheetStyle {
        require(shadow == null || (shadow.brush == null && shadow.blendMode == BlendMode.SrcOver)) {
            "A sheet's shadow is a colour drawn over what is beneath it"
        }
        return AppSheetStyle(material, colors, containerBrush, cornerRadius, floatingInset, maxWidth, shadow)
    }
}
