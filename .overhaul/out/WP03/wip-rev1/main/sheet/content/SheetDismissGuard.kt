package com.base.app.core.designsystem.component.sheet.content

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import com.base.app.core.designsystem.component.sheet.SheetDismissSource

/**
 * Keeps the enclosing sheet up while [enabled]: a drag down rubber-bands and springs back; the dim,
 * Back, Escape and the close button call [onAttempt] instead, once per attempt. Enable it only while
 * there is something to lose, so an untouched form keeps the system's predictive back. Closing from
 * code is not guarded. Outside a sheet it does nothing.
 */
@Composable
fun SheetDismissGuard(enabled: Boolean = true, onAttempt: (SheetDismissSource) -> Unit) {
    val slots = LocalSheetSlots.current ?: return
    val latest = rememberUpdatedState(onAttempt)
    // A disabled guard registers nothing, so the sheet's Back stays the plain predictive dismiss.
    if (!enabled) return
    DisposableEffect(slots) {
        val guard = Guard { source -> latest.value(source) }
        slots.add(guard)
        onDispose { slots.remove(guard) }
    }
}
