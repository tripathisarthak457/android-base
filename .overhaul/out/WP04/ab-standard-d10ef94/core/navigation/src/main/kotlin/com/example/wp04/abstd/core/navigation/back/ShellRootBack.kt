package com.example.wp04.abstd.core.navigation.back

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.example.wp04.abstd.core.navigation.ShellState

/** Back at the root of another tab selects the first tab; registered first, so asked last. */
@Composable
internal fun ShellRootBack(state: ShellState) {
    val idle = rememberNavigationEventState(NavigationEventInfo.None)
    val enabled by remember(state) { derivedStateOf { state.canBackAtRoot } }
    NavigationBackHandler(state = idle, isBackEnabled = enabled, onBackCompleted = { state.backAtRoot() })
}
