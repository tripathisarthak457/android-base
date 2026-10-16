package com.base.app.core.designsystem.component.sheet.surface

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.annotation.FrequentlyChangingValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/** The sheet's visible frame in its own coordinates. Getters are read in draw and allocate nothing. */
interface SheetFrame {
    @get:FrequentlyChangingValue
    val left: Float

    @get:FrequentlyChangingValue
    val top: Float

    @get:FrequentlyChangingValue
    val right: Float

    @get:FrequentlyChangingValue
    val bottom: Float

    val cornerRadius: Float
}

/**
 * Draws a Glass sheet's live body. The liquid glass feature provides it around the two hosts while
 * the tier samples the scene; everywhere else it is absent and a Glass sheet draws Frosted.
 */
@Stable
interface SheetBackdrop {
    /**
     * The scene behind [frame], blurred, under [dim] (the presenter's dim, which the recorded scene
     * does not contain) and a tint whose opacity is [opaque] (0 glass, 1 opaque). Both are read in
     * draw; the sampling pass is skipped while [opaque] is 1.
     */
    fun Modifier.sheetBackdrop(frame: SheetFrame, opaque: () -> Float, dim: () -> Color): Modifier
}

/** The backdrop Glass sheets draw with, or null where nothing samples the scene. */
val LocalSheetBackdrop: ProvidableCompositionLocal<SheetBackdrop?> = staticCompositionLocalOf { null }
