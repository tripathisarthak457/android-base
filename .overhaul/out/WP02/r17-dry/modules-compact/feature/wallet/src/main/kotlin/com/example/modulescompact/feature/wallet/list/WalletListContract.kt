package com.example.modulescompact.feature.wallet.list

import androidx.compose.runtime.Immutable
import com.example.modulescompact.core.common.LoadState
import com.example.modulescompact.core.mvi.UiEffect
import com.example.modulescompact.core.mvi.UiEvent
import com.example.modulescompact.core.mvi.UiState
import com.example.modulescompact.data.wallet.api.WalletItem

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
