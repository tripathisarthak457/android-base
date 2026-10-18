package com.example.wp04.dlplay.feature.sample.list

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import com.example.wp04.dlplay.core.common.LoadState
import com.example.wp04.dlplay.core.common.result.AppResult
import com.example.wp04.dlplay.core.mvi.MessageKind
import com.example.wp04.dlplay.core.mvi.MviViewModel
import com.example.wp04.dlplay.core.mvi.UiText
import com.example.wp04.dlplay.core.ui.toUiText
import com.example.wp04.dlplay.data.sample.api.SampleItem
import com.example.wp04.dlplay.data.sample.api.SampleRepository
import com.example.wp04.dlplay.feature.sample.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** The reference ViewModel. Every feature in this project is shaped like this one. */
@HiltViewModel
internal class SampleListViewModel @Inject constructor(
    private val repository: SampleRepository,
    savedStateHandle: SavedStateHandle,
) : MviViewModel<SampleListState, SampleListEvent, SampleListEffect>(SampleListState()) {

    /**
     * The search field. What the user typed survives the process being killed; the list does not,
     * because it is a request away and a restored one would be stale.
     */
    @OptIn(SavedStateHandleSaveableApi::class)
    val query: TextFieldState = savedStateHandle.saveable(QUERY, saver = TextFieldState.Saver) { TextFieldState() }

    init {
        observe(snapshotFlow { query.text.toString() }) { text -> copy(visibleItems = items.matching(text)) }
    }

    override fun handleEvent(event: SampleListEvent) {
        when (event) {
            SampleListEvent.Appeared -> if (current.loadState == LoadState.Idle) load(refreshing = false)
            SampleListEvent.Refresh -> load(refreshing = true)
            SampleListEvent.Retry -> load(refreshing = false)
            is SampleListEvent.ItemClicked -> emit(SampleListEffect.OpenDetail(event.id))
        }
    }

    private fun load(refreshing: Boolean) {
        launchLatest(LOAD) {
            setState { copy(loadState = if (refreshing) LoadState.Refreshing else LoadState.Loading) }

            when (val result = repository.items(forceRefresh = refreshing)) {
                is AppResult.Success -> setState {
                    copy(
                        loadState = if (result.value.isEmpty()) LoadState.Empty else LoadState.Success,
                        items = result.value,
                        visibleItems = result.value.matching(query.text.toString()),
                    )
                }

                // A failed refresh keeps the list on screen and reports it in a snackbar.
                is AppResult.Failure -> if (refreshing && current.items.isNotEmpty()) {
                    setState { copy(loadState = LoadState.Success) }
                    showMessage(
                        text = result.error.toUiText(UiText.of(R.string.sample_refresh_failed)),
                        kind = MessageKind.Error,
                    )
                } else {
                    setState { copy(loadState = LoadState.Error(result.error)) }
                }
            }
        }
    }

    private companion object {
        const val QUERY = "query"
        const val LOAD = "load"
    }
}

private fun List<SampleItem>.matching(query: String): List<SampleItem> {
    val wanted = query.trim()
    return if (wanted.isEmpty()) this else filter { it.title.contains(wanted, ignoreCase = true) }
}
