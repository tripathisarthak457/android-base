package com.example.finalafter.crui.feature.search

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import com.example.finalafter.crui.core.common.LoadState
import com.example.finalafter.crui.core.common.result.AppResult
import com.example.finalafter.crui.core.mvi.MviViewModel
import com.example.finalafter.crui.data.search.api.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
internal class SearchViewModel @Inject constructor(
    private val repository: SearchRepository,
    savedStateHandle: SavedStateHandle,
) : MviViewModel<SearchState, SearchEvent, SearchEffect>(SearchState()) {

    @OptIn(SavedStateHandleSaveableApi::class)
    val query: TextFieldState = savedStateHandle.saveable(QUERY, saver = TextFieldState.Saver) { TextFieldState() }

    private var lastRequested: String? = null

    init {
        observe(repository.recent) { copy(recent = it) }
        // Typing has to start a search, a side effect that `observe`'s pure reducer cannot run, so
        // the watcher runs from the screen's first start for as long as the ViewModel lives.
        // Between keystrokes it only checks each snapshot apply against the one field it reads.
        onFirstObserved {
            launchExclusive(TYPING) {
                snapshotFlow { query.text.toString() }.collect { search(it, debounce = true) }
            }
        }
    }

    override fun handleEvent(event: SearchEvent) {
        when (event) {
            SearchEvent.Submit -> {
                val text = query.text.toString()
                search(text, debounce = false)
                remember(text)
            }

            SearchEvent.Retry -> search(current.searchedFor, debounce = false)

            is SearchEvent.RecentClicked -> {
                query.setTextAndPlaceCursorAtEnd(event.query)
                search(event.query, debounce = false)
                remember(event.query)
            }

            is SearchEvent.RecentRemoved -> launchLatest(FORGET to event.query) { repository.forget(event.query) }
            SearchEvent.ClearRecent -> launchLatest(CLEAR) { repository.clearRecent() }

            // Navigation first: the tap must not wait for the recent-searches write to reach disk.
            is SearchEvent.ResultClicked -> {
                emit(SearchEffect.OpenResult(event.result.id))
                remember(current.searchedFor)
            }
        }
    }

    private fun search(text: String, debounce: Boolean) {
        // The field echoing back a query that was just searched immediately, after a recent
        // search was tapped, must not restart it behind a debounce.
        if (debounce && text == lastRequested) return
        lastRequested = text

        if (text.isBlank()) {
            cancel(SEARCH)
            setState { copy(searchedFor = "", loadState = LoadState.Idle, results = emptyList()) }
            return
        }
        launchLatest(SEARCH, debounce = if (debounce) DEBOUNCE else Duration.ZERO) {
            // Keeps the previous results on screen while the next ones load, so typing does not
            // flash the list empty between every keystroke.
            setState {
                copy(searchedFor = text, loadState = if (results.isEmpty()) LoadState.Loading else LoadState.Refreshing)
            }
            when (val result = repository.search(text)) {
                is AppResult.Success -> setState {
                    copy(
                        results = result.value,
                        loadState = if (result.value.isEmpty()) LoadState.Empty else LoadState.Success,
                    )
                }

                is AppResult.Failure -> setState {
                    copy(loadState = LoadState.Error(result.error), results = emptyList())
                }
            }
        }
    }

    // Keyed by the query, so only a repeat of the same write replaces itself. Under one shared key
    // the second of two quick writes would cancel the first, and DataStore drops an edit whose
    // caller is cancelled.
    private fun remember(text: String) {
        launchLatest(REMEMBER to text) { repository.remember(text) }
    }

    private companion object {
        const val QUERY = "query"
        const val TYPING = "typing"
        const val SEARCH = "search"
        const val REMEMBER = "remember"
        const val FORGET = "forget"
        const val CLEAR = "clear"
        val DEBOUNCE = 350.milliseconds
    }
}
