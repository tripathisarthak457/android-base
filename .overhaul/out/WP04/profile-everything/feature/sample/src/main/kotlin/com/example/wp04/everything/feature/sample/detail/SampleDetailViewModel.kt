package com.example.wp04.everything.feature.sample.detail

import com.example.wp04.everything.core.common.LoadState
import com.example.wp04.everything.core.common.result.AppResult
import com.example.wp04.everything.core.common.result.onSuccess
import com.example.wp04.everything.core.mvi.MviViewModel
import com.example.wp04.everything.data.sample.api.SampleItem
import com.example.wp04.everything.data.sample.api.SampleRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

/** The item id arrives through assisted injection, at construction, from the navigation key. */
@HiltViewModel(assistedFactory = SampleDetailViewModel.Factory::class)
internal class SampleDetailViewModel @AssistedInject constructor(
    private val repository: SampleRepository,
    @Assisted private val itemId: Int,
) : MviViewModel<SampleDetailState, SampleDetailEvent, SampleDetailEffect>(SampleDetailState()) {

    @AssistedFactory
    interface Factory {
        fun create(itemId: Int): SampleDetailViewModel
    }

    override fun handleEvent(event: SampleDetailEvent) {
        when (event) {
            SampleDetailEvent.Appeared -> if (current.loadState == LoadState.Idle) load()
            SampleDetailEvent.Retry -> load()
            SampleDetailEvent.BackClicked -> emit(SampleDetailEffect.NavigateBack)
            is SampleDetailEvent.RelatedClicked -> emit(SampleDetailEffect.OpenDetail(event.id))
        }
    }

    private fun load() {
        launchLatest(LOAD) {
            setState { copy(loadState = LoadState.Loading) }

            when (val result = repository.item(itemId)) {
                is AppResult.Success -> setState { copy(loadState = LoadState.Success, item = result.value) }
                is AppResult.Failure -> {
                    setState { copy(loadState = LoadState.Error(result.error)) }
                    return@launchLatest
                }
            }
            // The list is cached, so this costs no request after the list screen has loaded. A
            // failure only means no related rows: the item itself is already on screen.
            repository.items().onSuccess { all -> setState { copy(related = all.relatedTo(itemId)) } }
        }
    }

    private companion object {
        const val LOAD = "load"
    }
}

/**
 * The items that follow [id] in the list, wrapping at the end. Each detail screen's rows are a
 * different set, so pushing one from another keeps going deeper rather than returning to a screen
 * already in the stack.
 */
private fun List<SampleItem>.relatedTo(id: Int): List<SampleItem> {
    val index = indexOfFirst { it.id == id }
    val count = minOf(RELATED_COUNT, if (index < 0) size else size - 1)
    return (1..count).map { offset -> this[(index + offset).mod(size)] }
}

private const val RELATED_COUNT = 3
