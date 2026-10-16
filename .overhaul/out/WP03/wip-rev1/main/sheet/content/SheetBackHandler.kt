package com.base.app.core.designsystem.component.sheet.content

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState

/**
 * Takes Back (the key, a committed gesture, Escape) inside the enclosing sheet while [enabled],
 * instead of closing it: a step back in a flow the sheet holds. The sheet does not shrink for it.
 * Outside a sheet it does nothing; a screen there uses `NavigationBackHandler`.
 */
@Composable
fun SheetBackHandler(enabled: Boolean = true, onBack: () -> Unit) {
    val slots = LocalSheetSlots.current ?: return
    val latest = rememberUpdatedState(onBack)
    if (!enabled) return
    DisposableEffect(slots) {
        val step = StepBack { latest.value() }
        slots.add(step)
        onDispose { slots.remove(step) }
    }
}
