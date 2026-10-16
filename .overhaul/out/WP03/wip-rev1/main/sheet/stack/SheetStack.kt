package com.base.app.core.designsystem.component.sheet.stack

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.annotation.FrequentlyChangingValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.theme.tokens.DesignTokens

/**
 * The sheets up in a window, for whatever recedes behind them. The outermost `AppTheme` installs
 * one; every sheet registers itself with its level. Level 0 is the shell; a sheet's level is its
 * presenter's plus one, so a sheet opened from a sheet's content stacks on it.
 */
@Stable
class SheetStack internal constructor() {
    private val sheets = mutableStateListOf<StackedSheet>()
    private val claims = mutableStateListOf<Int>()

    /** How far the presenter at [level] has receded: 0 upright, 1 behind a sheet at Large. Read in a layer block. */
    @FrequentlyChangingValue
    fun recede(level: Int): Float {
        val sheet = front(level + 1) ?: return 0f
        return if (sheet.drivesCardStack) sheet.expansion() else 0f
    }

    /** The merged dim to draw over the presenter at [level], alpha included. Read in draw. */
    @FrequentlyChangingValue
    fun dim(level: Int): Color {
        val sheet = front(level + 1) ?: return Color.Transparent
        return CardStackMath.mergedDim(
            presence = sheet.dimPresence(),
            lift = recede(level),
            overlay = sheet.overlay,
            overlayAlpha = DesignTokens.Sheet.CardStack.overlayAlpha,
            scrim = sheet.scrim,
        )
    }

    /** True while the shell has receded past the point where the status bar turns light. Changes once per crossing. */
    val rootReceded: Boolean by derivedStateOf { recede(0) > DesignTokens.Sheet.CardStack.statusBarFlip }

    /** The front sheet's settled bottom chrome (its footer and the keyboard), for toasts to rest above. */
    val topBottomChrome: Dp by derivedStateOf { sheets.lastOrNull { it.active }?.bottomChrome ?: 0.dp }

    internal fun register(sheet: StackedSheet) {
        sheets += sheet
    }

    internal fun unregister(sheet: StackedSheet) {
        sheets -= sheet
    }

    /** A presenter draws the dim of the sheets above [level]; while one does, their scrims draw none. */
    internal fun claim(level: Int) {
        claims += level
    }

    internal fun release(level: Int) {
        claims -= level
    }

    internal fun isClaimed(level: Int): Boolean = level in claims

    /** True when [sheet] is the front-most sheet in the window: the one that takes Back, Escape and focus. */
    internal fun isFront(sheet: StackedSheet): Boolean = sheets.lastOrNull { it.active } === sheet

    /** The sheet covering [sheet], if one is up over it. */
    internal fun coverOf(sheet: StackedSheet): StackedSheet? = front(sheet.level + 1)

    /** The front sheet at [level], if any. */
    internal fun front(level: Int): StackedSheet? = sheets.lastOrNull { it.level == level && it.active }

    /** How many covered sheets are above [sheet] and active: beyond two it is not drawn (B-56). */
    internal fun depthAbove(sheet: StackedSheet): Int = sheets.count { it.active && it.level > sheet.level }
}

/** The outermost `AppTheme`'s stack, or null outside one (a bare preview). */
val LocalSheetStack: ProvidableCompositionLocal<SheetStack?> = staticCompositionLocalOf { null }

/**
 * One sheet as its stack sees it. The getters read the sheet's state, so a presenter reading
 * [SheetStack.recede] in a layer block follows the sheet frame by frame without recomposing.
 */
@Stable
internal class StackedSheet(
    val level: Int,
    private val isActive: () -> Boolean,
    private val expansionFraction: () -> Float,
    private val presence: () -> Float,
) {
    /** Modal, from the bottom, with Large active and room in height: the presenter recedes behind it. */
    var drivesCardStack by mutableStateOf(false)

    var overlay by mutableStateOf(Color.Black)
    var scrim by mutableStateOf(Color.Transparent)
    var bottomChrome by mutableStateOf(0.dp)

    /** Large's top for a sheet stacked on this one (B-54): this sheet's own Large top, lifted, plus the peek. */
    var largeTopForCover by mutableStateOf(Float.NaN)

    val active: Boolean get() = isActive()

    fun expansion(): Float = expansionFraction()

    fun dimPresence(): Float = presence()
}
