package com.example.cmp.feature.home.detail

import com.example.cmp.core.common.LoadState
import com.example.cmp.core.common.result.AppResult
import com.example.cmp.core.mvi.MviViewModel
import com.example.cmp.data.home.api.HomeRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

/** The id arrives through assisted injection, at construction, from the navigation key. */
@HiltViewModel(assistedFactory = HomeDetailViewModel.Factory::class)
internal class HomeDetailViewModel @AssistedInject constructor(
    private val repository: HomeRepository,
    @Assisted private val id: String,
) : MviViewModel<HomeDetailState, HomeDetailEvent, HomeDetailEffect>(
    initial = HomeDetailState(),
) {

    @AssistedFactory
    interface Factory {
        fun create(id: String): HomeDetailViewModel
    }

    init {
        onFirstObserved { load() }
    }

    override fun handleEvent(event: HomeDetailEvent) {
        when (event) {
            HomeDetailEvent.Retry -> load()

            HomeDetailEvent.BackClicked -> {
                emit(HomeDetailEffect.NavigateBack)
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
