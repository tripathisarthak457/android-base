package com.base.app.feature.sample.list

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import com.base.app.core.common.LoadState
import com.base.app.core.common.result.AppResult
import com.base.app.core.mvi.MessageKind
import com.base.app.core.mvi.MviViewModel
import com.base.app.core.mvi.UiText
import com.base.app.core.ui.toUiText
import com.base.app.data.sample.api.SampleRepository
import com.base.app.data.sample.api.model.SampleItem
import com.base.app.feature.sample.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

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
    val query: TextFieldState = savedStateHandle.saveable(QUERY, saver = TextFieldState.Saver) {
        TextFieldState()
    }

    init {
        observe(
            combine(
                repository.items,
                snapshotFlow {
                    query.text.toString()
                },
                ::Pair,
            ),
        ) { (items, text) ->
            copy(
                items = items,
                visibleItems = items.matching(text),
                // Rows already stored are shown while the refresh runs, instead of a skeleton.
                loadState = if (loadState == LoadState.Loading &&
                    items.isNotEmpty()
                ) {
                    LoadState.Refreshing
                } else {
                    loadState
                },
            )
        }
        onFirstObserved { load(refreshing = false) }
    }

    override fun handleEvent(event: SampleListEvent) {
        when (event) {
            SampleListEvent.Refresh -> load(refreshing = true)
            SampleListEvent.Retry -> load(refreshing = false)
            is SampleListEvent.ItemClicked -> emit(SampleListEffect.OpenDetail(event.id))
        }
    }

    private fun load(refreshing: Boolean) {
        launchLatest(LOAD) {
            setState {
                copy(
                    loadState = if (refreshing ||
                        items.isNotEmpty()
                    ) {
                        LoadState.Refreshing
                    } else {
                        LoadState.Loading
                    },
                )
            }

            val result = repository.refresh()
            // Read back rather than waiting for the observer, so Empty is never shown before the rows land.
            val stored = repository.items.first()
            when (result) {
                is AppResult.Success -> setState {
                    copy(
                        loadState = if (stored.isEmpty()) LoadState.Empty else LoadState.Success,
                        items = stored,
                        visibleItems = stored.matching(query.text.toString()),
                    )
                }

                // A failed refresh keeps the stored list on screen; one the person asked for says so.
                is AppResult.Failure -> if (stored.isNotEmpty()) {
                    setState {
                        copy(
                            loadState = LoadState.Success,
                            items = stored,
                            visibleItems = stored.matching(query.text.toString()),
                        )
                    }
                    if (refreshing) {
                        showMessage(
                            text = result.error.toUiText(UiText.of(R.string.sample_refresh_failed)),
                            kind = MessageKind.Error,
                        )
                    }
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
