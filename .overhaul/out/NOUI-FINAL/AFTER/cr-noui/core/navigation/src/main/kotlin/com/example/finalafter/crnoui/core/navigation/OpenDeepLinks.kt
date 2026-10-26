package com.example.finalafter.crnoui.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.filterNotNull

/**
 * Opens waiting links on [stack]: the key a link names is pushed over the screen on display, so
 * Back returns to it. A link to the screen already on top opens nothing new.
 */
@Composable
fun OpenDeepLinks(stack: AppBackStack, pending: PendingDeepLink) {
    LaunchedEffect(stack, pending) {
        pending.waiting.filterNotNull().collect {
            pending.take()?.let { key -> stack.applyNow(NavCommand.Navigate(key)) }
        }
    }
}
