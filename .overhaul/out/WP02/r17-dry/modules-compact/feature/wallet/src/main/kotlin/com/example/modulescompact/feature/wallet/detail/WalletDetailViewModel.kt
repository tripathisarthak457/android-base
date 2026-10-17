package com.example.modulescompact.feature.wallet.detail

import com.example.modulescompact.core.common.LoadState
import com.example.modulescompact.core.common.result.AppResult
import com.example.modulescompact.core.mvi.MviViewModel
import com.example.modulescompact.data.wallet.api.WalletRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

/** The id arrives through assisted injection, at construction, from the navigation key. */
@HiltViewModel(assistedFactory = WalletDetailViewModel.Factory::class)
internal class WalletDetailViewModel @AssistedInject constructor(
    private val repository: WalletRepository,
    @Assisted private val id: String,
) : MviViewModel<WalletDetailState, WalletDetailEvent, WalletDetailEffect>(
    initial = WalletDetailState(),
) {

    @AssistedFactory
    interface Factory {
        fun create(id: String): WalletDetailViewModel
    }

    override fun handleEvent(event: WalletDetailEvent) {
        when (event) {
            WalletDetailEvent.Appeared -> {
                if (current.loadState == LoadState.Idle) load()
            }

            WalletDetailEvent.Retry -> load()

            WalletDetailEvent.BackClicked -> {
                emit(WalletDetailEffect.NavigateBack)
            }
        }
    }

    private fun load() {
        launchLatest(LOAD) {
            setState { copy(loadState = LoadState.Loading) }
            when (val result = repository.item(id)) {
                is AppResult.Success -> setState {
                    copy(loadState = LoadState.Success, item = result.value)
                }

                is AppResult.Failure -> setState {
                    copy(loadState = LoadState.Error(result.error))
                }
            }
        }
    }

    private companion object {
        const val LOAD = "load"
    }
}
