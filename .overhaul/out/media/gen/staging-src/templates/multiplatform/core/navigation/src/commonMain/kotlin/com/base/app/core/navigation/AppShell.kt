package com.base.app.core.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.base.app.core.designsystem.component.navigation.AppTabBar
import com.base.app.core.designsystem.component.navigation.TabItem
import com.base.app.core.navigation.interim.StackHost
import org.jetbrains.compose.resources.stringResource

/**
 * The signed-in app: a tab bar over one navigation host per tab, with [navigator]'s commands
 * applied to the tab in front. Each tab's screens keep their saved state while another tab is in
 * front.
 */
@Composable
fun AppShell(
    tabs: List<ShellTab>,
    state: ShellState,
    registry: NavRegistry,
    navigator: AppNavigator,
    modifier: Modifier = Modifier,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(navigator, state, lifecycle) {
        navigator.applyWhileStarted(lifecycle) { command -> state.current.apply(command) }
    }

    val tabStates = rememberSaveableStateHolder()
    val items = tabs.map { TabItem(stringResource(it.label), it.icon, it.selectedIcon) }
    Column(modifier = modifier.fillMaxSize()) {
        val selected = state.selectedIndex
        tabStates.SaveableStateProvider(key = selected) {
            StackHost(
                backStack = state.stacks[selected],
                registry = registry,
                modifier = Modifier.weight(1f),
            )
        }
        AppTabBar(items = items, selected = selected, onSelect = state::select, onReselect = state::select)
    }
}
