package com.example.none.core.designsystem.component.sheet.stack

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.annotation.FrequentlyChangingValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.none.core.designsystem.foundation.layout.ShellSeamApi
import com.example.none.core.designsystem.theme.tokens.DesignTokens

/**
 * The sheets up in a window, for whatever recedes behind them. The outermost AppTheme installs one;
 * every sheet registers itself with its level. Level 0 is the shell; a modal sheet takes the level
 * above the highest sheet already up, so it stacks on whatever is in front, as iOS presents from the
 * top-most presented controller.
 *
 * Every per-frame function walks a small array with index loops: no iterator, filter or allocation
 * while a sheet moves.
 */
@ShellSeamApi
@Stable
class SheetStack internal constructor() {
    private var entries = arrayOfNulls<StackedSheet>(InitialCapacity)

    /** Snapshot state, so every reader that walks the entries follows a sheet arriving or leaving. */
    private var size by mutableIntStateOf(0)

    private val presenters = arrayOfNulls<PresenterGeometry>(Int.SIZE_BITS)

    /** One bit per level whose presenter draws its own dim: the shell's at bit 0, a cover's above it. */
    private var claimed by mutableIntStateOf(0)
    private val fold = DimFold()

    /** How far the presenter at [level] has receded: 0 upright, 1 behind a sheet at Large. Read in a layer block. */
    @FrequentlyChangingValue
    fun recede(level: Int): Float {
        val sheet = front(level + 1) ?: return 0f
        return if (sheet.drivesCardStack) sheet.expansion() else 0f
    }

    /** The merged dim to draw over the presenter at [level], alpha included. Read in draw. */
    @FrequentlyChangingValue
    fun dim(level: Int): Color {
        fold.reset()
        val above = front(level + 1) ?: return Color.Transparent
        fold.over(above.overlay, DesignTokens.Sheet.CardStack.overlayAlpha * recede(level))
        var reach = level + 1
        while (true) {
            val sheet = front(reach) ?: break
            if (!sheet.paneBounded) fold.over(sheet.scrim, sheet.scrim.alpha * sheet.presence())
            if (sheet.modal) break
            reach++
        }
        return fold.color()
    }

    /**
     * The presenter at [level]'s visible bottom in its own px: its full height (infinity), or, while
     * an opaque sheet above it is at Large, that sheet's top plus its corner radius, below which
     * nothing of the presenter shows. Read in draw.
     */
    @FrequentlyChangingValue
    fun visibleBottom(level: Int): Float {
        val sheet = front(level + 1) ?: return Float.POSITIVE_INFINITY
        if (!sheet.coversPresenter()) return Float.POSITIVE_INFINITY
        val geometry = presenters.getOrNull(level) ?: return Float.POSITIVE_INFINITY
        val expansion = if (geometry.reduceMotion) 0f else recede(level)
        val top = sheet.visibleTop() - geometry.windowTop - CardStackMath.translationY(expansion, geometry.safeTop)
        return top / CardStackMath.scale(expansion) + sheet.cornerRadius
    }

    /** True while a sheet at [level] may sample the live scene: its presenter is the shell and no cover is up. */
    fun samplesScene(level: Int): Boolean = level == 1 && claimed and 1.inv() == 0

    /**
     * True while a modal sheet is up above [level], so the default-priority Back handlers of the hosts
     * at that level stay off. A destination sheet stops blocking once it leaves, so the presses
     * of a rapid Back during its exit reach its host's gate. Changes at most twice per opening.
     */
    fun blocksBack(level: Int): Boolean {
        for (index in 0 until size) {
            val sheet = entries[index] ?: continue
            if (sheet.level > level && sheet.modal && sheet.blocks()) return true
        }
        return false
    }

    /** True while the shell has receded past sheet.cardStack.statusBarFlip. Changes once per crossing. */
    val rootReceded: Boolean by derivedStateOf { recede(0) > DesignTokens.Sheet.CardStack.statusBarFlip }

    /** The container of the sheet covering the window's bottom edge, or null. Changes once per crossing. */
    val bottomEdgeContainer: Color? by derivedStateOf {
        var container: Color? = null
        for (index in 0 until size) {
            val sheet = entries[index] ?: continue
            if (sheet.coversBottomEdge()) container = sheet.container
        }
        container
    }

    /** The front sheet's settled bottom chrome, for toasts. */
    val topBottomChrome: Dp by derivedStateOf {
        var chrome = 0.dp
        for (index in 0 until size) {
            val sheet = entries[index] ?: continue
            if (sheet.active()) chrome = sheet.bottomChrome
        }
        chrome
    }

    /** Registers [sheet] in stack order and returns its level. */
    internal fun register(sheet: StackedSheet, ownerLevel: Int): Int {
        for (index in 0 until size) if (entries[index] === sheet) return sheet.level
        var highest = ownerLevel
        for (index in 0 until size) {
            val other = entries[index] ?: continue
            if (other.active() && other.level > highest) highest = other.level
        }
        sheet.level = if (sheet.modal) highest + 1 else ownerLevel + 1
        if (size == entries.size) entries = entries.copyOf(size * 2)
        entries[size] = sheet
        size++
        return sheet.level
    }

    internal fun unregister(sheet: StackedSheet) {
        var at = -1
        for (index in 0 until size) if (entries[index] === sheet) at = index
        if (at < 0) return
        for (index in at until size - 1) entries[index] = entries[index + 1]
        entries[size - 1] = null
        size--
    }

    /** The sheet stacked on [sheet], if one is up over it. */
    internal fun coverOf(sheet: StackedSheet): StackedSheet? {
        var after = false
        for (index in 0 until size) {
            val other = entries[index] ?: continue
            if (other === sheet) {
                after = true
            } else if (after && other.active() && other.level > sheet.level) {
                return other
            }
        }
        return null
    }

    /** How many sheets are up in front of [sheet]: beyond two it is not drawn. */
    internal fun depthAbove(sheet: StackedSheet): Int {
        var depth = 0
        for (index in 0 until size) {
            val other = entries[index] ?: continue
            if (other !== sheet && other.active() && other.level > sheet.level) depth++
        }
        return depth
    }

    /** True when [sheet] is the front-most sheet up: the one that takes Back, Escape and focus. */
    internal fun isFront(sheet: StackedSheet): Boolean {
        for (index in size - 1 downTo 0) {
            val other = entries[index] ?: continue
            if (other.active()) return other === sheet
        }
        return false
    }

    /** The front-most sheet up at [level]. */
    internal fun front(level: Int): StackedSheet? {
        for (index in size - 1 downTo 0) {
            val sheet = entries[index] ?: continue
            if (sheet.level == level && sheet.active()) return sheet
        }
        return null
    }

    /** A presenter at [level] draws the dim that reaches it; while one does, the sheet above draws none itself. */
    internal fun claim(level: Int, geometry: PresenterGeometry) {
        if (level !in presenters.indices) return
        presenters[level] = geometry
        claimed = claimed or (1 shl level)
    }

    internal fun release(level: Int, geometry: PresenterGeometry) {
        if (level !in presenters.indices || presenters[level] !== geometry) return
        presenters[level] = null
        claimed = claimed and (1 shl level).inv()
    }

    internal fun isClaimed(level: Int): Boolean = level in presenters.indices && claimed and (1 shl level) != 0

    private companion object {
        const val InitialCapacity = 8
    }
}

/**
 * Where a presenter is, written by its node in placement: plain fields, read by the stack while it
 * draws, so nothing here is snapshot state.
 */
internal class PresenterGeometry {
    var windowTop = 0f
    var safeTop = 0f
    var reduceMotion = false
}

/** The outermost AppTheme's stack, or null outside one (a bare preview). */
@ShellSeamApi
val LocalSheetStack: ProvidableCompositionLocal<SheetStack?> = staticCompositionLocalOf { null }
