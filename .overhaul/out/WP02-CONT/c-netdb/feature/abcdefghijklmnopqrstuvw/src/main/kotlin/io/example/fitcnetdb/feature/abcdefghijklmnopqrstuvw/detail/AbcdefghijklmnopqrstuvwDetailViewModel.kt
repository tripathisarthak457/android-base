package io.example.fitcnetdb.feature.abcdefghijklmnopqrstuvw.detail

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.example.fitcnetdb.core.common.LoadState
import io.example.fitcnetdb.core.common.result.AppResult
import io.example.fitcnetdb.core.mvi.MviViewModel
import io.example.fitcnetdb.data.abcdefghijklmnopqrstuvw.api.AbcdefghijklmnopqrstuvwRepository

/** The id arrives through assisted injection, at construction, from the navigation key. */
@HiltViewModel(assistedFactory = AbcdefghijklmnopqrstuvwDetailViewModel.Factory::class)
internal class AbcdefghijklmnopqrstuvwDetailViewModel @AssistedInject constructor(
    private val repository: AbcdefghijklmnopqrstuvwRepository,
    @Assisted private val id: String,
) : MviViewModel<
    AbcdefghijklmnopqrstuvwDetailState,
    AbcdefghijklmnopqrstuvwDetailEvent,
    AbcdefghijklmnopqrstuvwDetailEffect,
    >(
    initial = AbcdefghijklmnopqrstuvwDetailState(),
) {

    @AssistedFactory
    interface Factory {
        fun create(id: String): AbcdefghijklmnopqrstuvwDetailViewModel
    }

    override fun handleEvent(event: AbcdefghijklmnopqrstuvwDetailEvent) {
        when (event) {
            AbcdefghijklmnopqrstuvwDetailEvent.Appeared -> {
                if (current.loadState == LoadState.Idle) load()
            }

            AbcdefghijklmnopqrstuvwDetailEvent.Retry -> load()

            AbcdefghijklmnopqrstuvwDetailEvent.BackClicked -> {
                emit(AbcdefghijklmnopqrstuvwDetailEffect.NavigateBack)
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
