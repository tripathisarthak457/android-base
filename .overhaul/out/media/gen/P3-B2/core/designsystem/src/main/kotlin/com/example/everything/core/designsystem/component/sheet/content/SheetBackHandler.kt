package com.example.everything.core.designsystem.component.sheet.content

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.example.everything.core.designsystem.foundation.layout.LocalEntryVisibility

/**
 * Takes Back (the key, a committed gesture, Escape) inside the enclosing sheet while [enabled],
 * instead of closing it: a step back in a flow the sheet holds. The sheet does not shrink for it.
 */
@Composable
fun SheetBackHandler(enabled: Boolean = true, onBack: () -> Unit) {
    val slots = LocalSheetSlots.current ?: return
    if (!enabled) return
    val visibility = LocalEntryVisibility.current
    val latest by rememberUpdatedState(onBack)
    val step = remember(slots) { StepBack({ latest() }) { visibility.isUncovered } }
    DisposableEffect(slots, step) {
        slots.add(step)
        onDispose { slots.remove(step) }
    }
}
