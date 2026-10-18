package com.example.wp04.base.feature.wallet

import com.example.wp04.base.core.common.AppResult
import com.example.wp04.base.core.common.mvi.LoadState
import com.example.wp04.base.core.common.mvi.MviViewModel
import com.example.wp04.base.data.wallet.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Loads in `init`, which runs once per screen, rather than from a `LaunchedEffect`. */
@HiltViewModel
class WalletViewModel @Inject constructor(
    private val repository: WalletRepository,
) : MviViewModel<WalletState, WalletEvent, WalletEffect>(WalletState()) {
    init {
        onEvent(WalletEvent.Load)
    }

    override suspend fun handleEvent(event: WalletEvent) {
        when (event) {
            WalletEvent.Load, WalletEvent.Retry -> load()
            is WalletEvent.ItemClicked -> emitEffect(WalletEffect.OpenDetail(event.id))
        }
    }

    private suspend fun load() {
        updateState { copy(loadState = LoadState.Loading) }

        when (val result = repository.items()) {
            is AppResult.Success -> updateState {
                copy(
                    loadState = if (result.data.isEmpty()) LoadState.Empty else LoadState.Success,
                    items = result.data,
                )
            }

            is AppResult.Failure -> updateState { copy(loadState = result.toLoadState()) }
        }
    }
}
