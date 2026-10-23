package com.example.lean.feature.home.list

import com.example.lean.core.common.LoadState
import com.example.lean.core.common.result.AppResult
import com.example.lean.core.mvi.MessageKind
import com.example.lean.core.mvi.MviViewModel
import com.example.lean.core.ui.toUiText
import com.example.lean.data.home.api.HomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
internal class HomeListViewModel @Inject constructor(private val repository: HomeRepository) :
    MviViewModel<HomeListState, HomeListEvent, HomeListEffect>(
        initial = HomeListState(),
    ) {

    override fun handleEvent(event: HomeListEvent) {
        when (event) {
            HomeListEvent.Appeared -> {
                if (current.loadState == LoadState.Idle) load(refreshing = false)
            }

            HomeListEvent.Refresh -> load(refreshing = true)

            HomeListEvent.Retry -> load(refreshing = false)

            is HomeListEvent.ItemClicked -> {
                emit(HomeListEffect.OpenDetail(event.id))
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
