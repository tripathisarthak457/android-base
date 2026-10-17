package com.example.modulesmodular.feature.wallet.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.modulesmodular.core.navigation.AppNavigator
import com.example.modulesmodular.core.ui.MviScreen

@Composable
internal fun WalletDetailRoute(
    id: String,
    navigator: AppNavigator,
    viewModel: WalletDetailViewModel =
        hiltViewModel<WalletDetailViewModel, WalletDetailViewModel.Factory>(
            creationCallback = { factory -> factory.create(id) },
        ),
) {
    LaunchedEffect(Unit) { viewModel.onEvent(WalletDetailEvent.Appeared) }

    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                WalletDetailEffect.NavigateBack -> navigator.navigateUp()
            }
        },
    ) { state, onEvent ->
        WalletDetailScreen(state = state, onEvent = onEvent)
    }
}
