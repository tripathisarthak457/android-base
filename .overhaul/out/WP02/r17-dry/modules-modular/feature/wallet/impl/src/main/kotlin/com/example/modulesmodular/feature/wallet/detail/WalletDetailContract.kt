package com.example.modulesmodular.feature.wallet.detail

import androidx.compose.runtime.Immutable
import com.example.modulesmodular.core.common.LoadState
import com.example.modulesmodular.core.mvi.UiEffect
import com.example.modulesmodular.core.mvi.UiEvent
import com.example.modulesmodular.core.mvi.UiState
import com.example.modulesmodular.data.wallet.api.WalletItem

@Immutable
internal data class WalletDetailState(
    val loadState: LoadState = LoadState.Idle,
    val item: WalletItem? = null,
) : UiState

internal sealed interface WalletDetailEvent : UiEvent {
    data object Appeared : WalletDetailEvent
    data object Retry : WalletDetailEvent
    data object BackClicked : WalletDetailEvent
}

internal sealed interface WalletDetailEffect : UiEffect {
    data object NavigateBack : WalletDetailEffect
}
