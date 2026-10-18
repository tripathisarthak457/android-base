package com.example.wp04.base.feature.wallet

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.wp04.base.core.navigation.AppNavigator
import com.example.wp04.base.core.ui.MviScreen

/** Connects the ViewModel, the screen and navigation, keeping the screen previewable. */
@Composable
fun WalletListRoute(
    navigator: AppNavigator,
    viewModel: WalletViewModel = hiltViewModel(),
) {
    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                is WalletEffect.OpenDetail -> navigator.navigate(WalletDetailKey(effect.id))
            }
        },
    ) { state, onEvent ->
        WalletScreen(state = state, onEvent = onEvent)
    }
}
