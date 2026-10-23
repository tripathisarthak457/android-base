package io.example.fitcnetdb.core.designsystem.component.sheet.content

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import io.example.fitcnetdb.core.designsystem.component.sheet.SheetDismissSource
import io.example.fitcnetdb.core.designsystem.foundation.layout.LocalEntryVisibility

/**
 * Keeps the enclosing sheet up while [enabled]: a drag down rubber-bands and springs back; the dim,
 * Back, Escape and the close button call [onAttempt] instead, once per attempt. Enable it only while
 * there is something to lose, so an untouched form keeps the system's predictive back. Closing from
 * code is not guarded. Outside a sheet it does nothing.
 */
@Composable
fun SheetDismissGuard(enabled: Boolean = true, onAttempt: (SheetDismissSource) -> Unit) {
    val slots = LocalSheetSlots.current ?: return
    if (!enabled) return
    val visibility = LocalEntryVisibility.current
    val latest by rememberUpdatedState(onAttempt)
    val guard = remember(slots) { Guard({ latest(it) }) { visibility.isUncovered } }
    DisposableEffect(slots, guard) {
        slots.add(guard)
        onDispose { slots.remove(guard) }
    }
}
