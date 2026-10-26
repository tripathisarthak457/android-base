package com.base.app.feature.search.result

import com.base.app.core.common.LoadState
import com.base.app.core.common.result.AppResult
import com.base.app.core.mvi.MviViewModel
import com.base.app.data.search.api.SearchRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel(assistedFactory = SearchResultViewModel.Factory::class)
internal class SearchResultViewModel @AssistedInject constructor(
    private val repository: SearchRepository,
    @Assisted private val id: Int,
) : MviViewModel<SearchResultState, SearchResultEvent, SearchResultEffect>(SearchResultState()) {

    @AssistedFactory
    interface Factory {
        fun create(id: Int): SearchResultViewModel
    }

    init {
        onFirstObserved { load() }
    }

    override fun handleEvent(event: SearchResultEvent) {
        when (event) {
            SearchResultEvent.Retry -> load()
            SearchResultEvent.BackClicked -> emit(SearchResultEffect.NavigateBack)
        }
    }

    private fun load() {
        launchLatest(LOAD) {
            setState { copy(loadState = LoadState.Loading) }
            when (val result = repository.result(id)) {
                is AppResult.Success -> setState {
                    copy(loadState = LoadState.Success, result = result.value)
                }

                is AppResult.Failure -> setState { copy(loadState = LoadState.Error(result.error)) }
            }
        }
    }

    private companion object {
        const val LOAD = "load"
    }
}
