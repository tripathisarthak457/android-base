package com.example.modulesmodular.feature.wallet.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.modulesmodular.core.navigation.AppNavigator
import com.example.modulesmodular.core.ui.MviScreen
import com.example.modulesmodular.feature.wallet.api.WalletDetailKey

@Composable
internal fun WalletListRoute(
    navigator: AppNavigator,
    viewModel: WalletListViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) { viewModel.onEvent(WalletListEvent.Appeared) }

    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                is WalletListEffect.OpenDetail -> {
                    navigator.navigate(WalletDetailKey(effect.id))
                }
            }
        },
    ) { state, onEvent ->
        WalletListScreen(state = state, onEvent = onEvent)
    }
}
