package com.example.deskstandard.core.navigation.back

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigationevent.NavigationEventHandler
import androidx.navigationevent.NavigationEventInfo
import com.example.deskstandard.core.navigation.ShellState

/** Back at the root of another tab selects the first tab; registered first, so asked last. */
@Composable
internal fun ShellRootBack(state: ShellState) {
    val handler = remember(state) { RootBack(state) }
    BackHandlerEffect(handler) { state.canBackAtRoot }
}

private class RootBack(private val state: ShellState) :
    NavigationEventHandler<NavigationEventInfo>(NavigationEventInfo.None, isBackEnabled = false) {

    override fun onBackCompleted() {
        state.backAtRoot()
    }
}
