package com.example.finalafter.crui.core.navigation.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import com.example.finalafter.crui.core.navigation.PendingDeepLink
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

/**
 * Opens waiting links in the signed-in shell: a tab's root selects that tab; another key selects the
 * tab whose feature registered it and is pushed there, or presented above it.
 */
@Composable
fun OpenDeepLinks(state: ShellState, pending: PendingDeepLink) {
    LaunchedEffect(state, pending) {
        pending.waiting.filterNotNull().collect {
            snapshotFlow { state.moving }.first { !it }
            pending.take()?.let(state::open)
        }
    }
}
