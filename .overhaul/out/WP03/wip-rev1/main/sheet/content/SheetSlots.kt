package com.base.app.core.designsystem.component.sheet.content

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.base.app.core.designsystem.component.sheet.AppSheetState
import com.base.app.core.designsystem.component.sheet.SheetDismissSource
import com.base.app.core.designsystem.component.sheet.SheetStop

/**
 * What the content of a sheet has asked of it: a header or footer to show instead of the call
 * site's, a guard on closing, a step back to take before closing. The innermost (last registered)
 * of each wins, so a flow inside a sheet can replace what its outer screen set.
 *
 * A registration made in the sheet's first composition holds its arrival until the slot is
 * composed and measured (B-2), so a fit-to-content sheet rises to a height that includes it.
 */
internal class SheetSlots(private val state: AppSheetState) {
    private val headers = mutableStateListOf<SlotContent>()
    private val footers = mutableStateListOf<SlotContent>()
    private val guards = mutableStateListOf<Guard>()
    private val backs = mutableStateListOf<StepBack>()

    /** The header content provides, read where the header slot composes. */
    val header: SlotContent? get() = headers.lastOrNull()

    val footer: SlotContent? get() = footers.lastOrNull()

    /** The innermost enabled guard; while there is one, the person cannot close the sheet. */
    val guard: Guard? get() = guards.lastOrNull()

    val stepBack: StepBack? get() = backs.lastOrNull()

    fun add(slot: SlotContent, footer: Boolean) {
        if (!state.isVisible || state.anchored.settledValue == SheetStop.Hidden) {
            state.awaitingSlots = true
        }
        if (footer) footers += slot else headers += slot
    }

    fun remove(slot: SlotContent) {
        headers -= slot
        footers -= slot
    }

    fun add(guard: Guard) {
        guards += guard
    }

    fun remove(guard: Guard) {
        guards -= guard
    }

    fun add(step: StepBack) {
        backs += step
    }

    fun remove(step: StepBack) {
        backs -= step
    }

    /** Called once a slot host has composed what was registered: the sheet may rise now. */
    fun composed() {
        if (state.awaitingSlots) state.awaitingSlots = false
    }
}

/** Header or footer content provided from inside a sheet; [content] follows the provider's latest. */
internal class SlotContent(content: @Composable () -> Unit) {
    var content: @Composable () -> Unit by mutableStateOf(content)
}

internal class Guard(val onAttempt: (SheetDismissSource) -> Unit)

internal class StepBack(val onBack: () -> Unit)

internal val LocalSheetSlots = staticCompositionLocalOf<SheetSlots?> { null }

/** Puts [content] in the enclosing sheet's header slot, replacing `header`. Outside a sheet it draws in place. */
@Composable
fun ProvideSheetHeader(content: @Composable () -> Unit) = ProvideSlot(footer = false, content)

/** Puts [content] in the enclosing sheet's footer slot, pinned to its visible bottom. In place outside a sheet. */
@Composable
fun ProvideSheetFooter(content: @Composable () -> Unit) = ProvideSlot(footer = true, content)

@Composable
private fun ProvideSlot(footer: Boolean, content: @Composable () -> Unit) {
    val slots = LocalSheetSlots.current
    if (slots == null) {
        content()
        return
    }
    val slot = remember { SlotContent(content) }
    SideEffect { slot.content = content }
    DisposableEffect(slots, slot) {
        slots.add(slot, footer)
        onDispose { slots.remove(slot) }
    }
}
