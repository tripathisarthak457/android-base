package io.example.fitcnet.core.designsystem.component.sheet.surface

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.annotation.FrequentlyChangingValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.example.fitcnet.core.designsystem.foundation.layout.ShellSeamApi

/**
 * The sheet's visible frame in its own coordinates, and what its live body needs. Non-generic
 * getters, so nothing boxes; read in draw; nothing allocates.
 */
@ShellSeamApi
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

    /** 0 glass, 1 opaque: the pass is skipped at 1, and whenever the stack says this level must not sample. */
    @get:FrequentlyChangingValue
    val opaque: Float

    /** The presenter's dim, which the recorded scene does not contain. */
    @get:FrequentlyChangingValue
    val dim: Color
}

/** Draws a Glass sheet's live body: the scene behind the frame, blurred, under the dim and a tint. */
@ShellSeamApi
@Stable
interface SheetBackdrop {
    fun Modifier.sheetBackdrop(frame: SheetFrame): Modifier
}

/**
 * One per window, installed by the outermost AppTheme, stable for the window's life. The liquid
 * glass stage writes [backdrop] when it attaches and when the glass tier changes; null makes Glass
 * draw Frosted. A holder rather than a composition local of its own, because the stage sits inside
 * the shell while sheets render in hosts around it, and swapping a local on a tier change would
 * recompose every sheet's content at the moment the GPU fell behind.
 */
@ShellSeamApi
@Stable
class SheetBackdropHolder internal constructor() {
    var backdrop: SheetBackdrop? by mutableStateOf(null)
}

@ShellSeamApi
val LocalSheetBackdrop: ProvidableCompositionLocal<SheetBackdropHolder?> = staticCompositionLocalOf { null }
