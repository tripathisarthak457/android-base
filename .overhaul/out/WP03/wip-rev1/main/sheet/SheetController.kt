package com.base.app.core.designsystem.component.sheet

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.component.sheet.content.SheetSlots
import com.base.app.core.designsystem.component.sheet.gesture.SheetNestedScroll
import com.base.app.core.designsystem.component.sheet.stack.SheetStack
import com.base.app.core.designsystem.component.sheet.stack.StackedSheet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

/**
 * One sheet's behaviour around its state: the scope its content sees, the decisions about closing
 * the state consults, and the registrations its content makes. Built once per sheet; the
 * parameters of the latest composition are written into it after each composition.
 */
@Stable
internal class SheetController(override val sheetState: AppSheetState, private val scope: CoroutineScope) :
    AppSheetScope, SheetBehaviour {

    val slots = SheetSlots(sheetState)
    val padding = SheetContentPadding()
    var resizes by mutableStateOf(true)
    val nestedScroll = SheetNestedScroll(sheetState) { resizes }

    var dismissible by mutableStateOf(true)
    var scrimTapDismisses = true
    var onDismissRequest: () -> Unit = {}
    var onDetentChange: ((SheetDetent) -> Unit)? = null
    var stack: SheetStack? = null
    var stacked: StackedSheet? = null

    /** How many pixels of scroll the header's edge fade takes to appear, written in measure. */
    var scrollEdgeRampPx by mutableFloatStateOf(1f)

    override val contentPadding: PaddingValues get() = padding

    override val contentScrolled: Float
        get() = (nestedScroll.scrolled / scrollEdgeRampPx).coerceIn(0f, 1f)

    override val isCovered: Boolean
        get() {
            val stacked = stacked ?: return false
            return stack?.coverOf(stacked) != null
        }

    override val dismissAllowed: Boolean
        get() = dismissible && slots.guard == null

    override fun requestDismiss() = attempt(SheetDismissSource.Request)

    override fun dismiss() = close(SheetDismissSource.Request)

    /** The person tried to close the sheet by [source]: it closes, or the guard hears of it. */
    fun attempt(source: SheetDismissSource) {
        if (!sheetState.isVisible || sheetState.leaving) return
        if (dismissAllowed) close(source) else attempted(source)
    }

    override fun dismissed(source: SheetDismissSource) {
        if (!sheetState.leaving) onDismissRequest()
    }

    override fun attempted(source: SheetDismissSource) {
        slots.guard?.onAttempt?.invoke(source)
    }

    override fun detentChanged(detent: SheetDetent) {
        onDetentChange?.invoke(detent)
    }

    private fun close(source: SheetDismissSource) {
        if (!sheetState.isVisible || sheetState.leaving) return
        dismissed(source)
        // Undispatched, so the sheet is leaving before this returns and a second press is ignored.
        scope.launch(start = CoroutineStart.UNDISPATCHED) { sheetState.leave(velocity = 0f) }
    }
}

/**
 * A sheet's content padding as the scroller reads it: snapshot state read in measure, so a new
 * value re-measures the scroller without recomposing it.
 */
@Stable
internal class SheetContentPadding : PaddingValues {
    var bottom: Dp by mutableStateOf(0.dp)

    override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp = 0.dp

    override fun calculateTopPadding(): Dp = 0.dp

    override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp = 0.dp

    override fun calculateBottomPadding(): Dp = bottom
}
