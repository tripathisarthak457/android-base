package com.example.modulesmodular.feature.wallet.list

import com.example.modulesmodular.core.common.LoadState
import com.example.modulesmodular.core.common.result.AppResult
import com.example.modulesmodular.core.mvi.MessageKind
import com.example.modulesmodular.core.mvi.MviViewModel
import com.example.modulesmodular.core.ui.toUiText
import com.example.modulesmodular.data.wallet.api.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
internal class WalletListViewModel @Inject constructor(private val repository: WalletRepository) :
    MviViewModel<WalletListState, WalletListEvent, WalletListEffect>(
        initial = WalletListState(),
    ) {

    override fun handleEvent(event: WalletListEvent) {
        when (event) {
            WalletListEvent.Appeared -> {
                if (current.loadState == LoadState.Idle) load(refreshing = false)
            }

            WalletListEvent.Refresh -> load(refreshing = true)

            WalletListEvent.Retry -> load(refreshing = false)

            is WalletListEvent.ItemClicked -> {
                emit(WalletListEffect.OpenDetail(event.id))
            }
        }
    }

    private fun load(refreshing: Boolean) {
        launchLatest(LOAD) {
            val loading = if (refreshing) LoadState.Refreshing else LoadState.Loading
            setState { copy(loadState = loading) }

            when (val result = repository.items(forceRefresh = refreshing)) {
                is AppResult.Success -> setState {
                    val loaded = if (result.value.isEmpty()) LoadState.Empty else LoadState.Success
                    copy(loadState = loaded, items = result.value)
                }

                // A failed refresh keeps the rows on screen and says so in a message.
                is AppResult.Failure -> if (refreshing && current.items.isNotEmpty()) {
                    setState { copy(loadState = LoadState.Success) }
                    showMessage(text = result.error.toUiText(), kind = MessageKind.Error)
                } else {
                    setState { copy(loadState = LoadState.Error(result.error)) }
                }
            }
        }
    }

    private companion object {
        const val LOAD = "load"
    }
}
