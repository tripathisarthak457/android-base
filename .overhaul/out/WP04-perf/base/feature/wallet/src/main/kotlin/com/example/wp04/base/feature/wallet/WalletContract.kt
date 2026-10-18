package com.example.wp04.base.feature.wallet

import androidx.compose.runtime.Immutable
import com.example.wp04.base.core.common.mvi.LoadState
import com.example.wp04.base.core.common.mvi.UiEffect
import com.example.wp04.base.core.common.mvi.UiEvent
import com.example.wp04.base.core.common.mvi.UiState
import com.example.wp04.base.data.wallet.WalletItem

@Immutable
data class WalletState(
    val loadState: LoadState = LoadState.Idle,
    val items: List<WalletItem> = emptyList(),
) : UiState

sealed interface WalletEvent : UiEvent {
    data object Load : WalletEvent
    data object Retry : WalletEvent
    data class ItemClicked(val id: Int) : WalletEvent
}

sealed interface WalletEffect : UiEffect {
    data class OpenDetail(val id: Int) : WalletEffect
}
