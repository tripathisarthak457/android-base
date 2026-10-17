package com.example.modulesmodular.feature.wallet.list

import androidx.compose.runtime.Immutable
import com.example.modulesmodular.core.common.LoadState
import com.example.modulesmodular.core.mvi.UiEffect
import com.example.modulesmodular.core.mvi.UiEvent
import com.example.modulesmodular.core.mvi.UiState
import com.example.modulesmodular.data.wallet.api.WalletItem

@Immutable
internal data class WalletListState(
    val loadState: LoadState = LoadState.Idle,
    val items: List<WalletItem> = emptyList(),
) : UiState

internal sealed interface WalletListEvent : UiEvent {
    data object Appeared : WalletListEvent
    data object Refresh : WalletListEvent
    data object Retry : WalletListEvent
    data class ItemClicked(val id: String) : WalletListEvent
}

internal sealed interface WalletListEffect : UiEffect {
    data class OpenDetail(val id: String) : WalletListEffect
}
