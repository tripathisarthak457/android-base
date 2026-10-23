package io.example.fitclocal.feature.abcdefghijklmno.list

import dagger.hilt.android.lifecycle.HiltViewModel
import io.example.fitclocal.core.common.LoadState
import io.example.fitclocal.core.common.result.AppResult
import io.example.fitclocal.core.mvi.MessageKind
import io.example.fitclocal.core.mvi.MviViewModel
import io.example.fitclocal.core.ui.toUiText
import io.example.fitclocal.data.abcdefghijklmno.api.AbcdefghijklmnoRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

@HiltViewModel
internal class AbcdefghijklmnoListViewModel @Inject constructor(
    private val repository: AbcdefghijklmnoRepository,
) : MviViewModel<AbcdefghijklmnoListState, AbcdefghijklmnoListEvent, AbcdefghijklmnoListEffect>(
    initial = AbcdefghijklmnoListState(),
) {

    init {
        // Every write to the list reaches the screen, not only the ones a refresh makes.
        observe(repository.items) { items ->
            copy(items = items, loadState = loadState.holding(items.isNotEmpty()))
        }
    }

    override fun handleEvent(event: AbcdefghijklmnoListEvent) {
        when (event) {
            AbcdefghijklmnoListEvent.Appeared -> {
                if (current.loadState == LoadState.Idle) load(refreshing = false)
            }

            AbcdefghijklmnoListEvent.Refresh -> load(refreshing = true)

            AbcdefghijklmnoListEvent.Retry -> load(refreshing = false)

            is AbcdefghijklmnoListEvent.ItemClicked -> {
                emit(AbcdefghijklmnoListEffect.OpenDetail(event.id))
            }
        }
    }

    private fun load(refreshing: Boolean) {
        launchLatest(LOAD) {
            setState {
                val showing = refreshing || items.isNotEmpty()
                copy(loadState = if (showing) LoadState.Refreshing else LoadState.Loading)
            }

            val result = repository.refresh()
            // Read back rather than wait for the observer, so Empty never shows before rows land.
            val stored = repository.items.first()
            when (result) {
                is AppResult.Success -> setState {
                    val loaded = if (stored.isEmpty()) LoadState.Empty else LoadState.Success
                    copy(loadState = loaded, items = stored)
                }

                // A failed refresh keeps stored rows on screen; one the person asked for says so.
                is AppResult.Failure -> if (stored.isNotEmpty()) {
                    setState { copy(loadState = LoadState.Success, items = stored) }
                    if (refreshing) {
                        showMessage(text = result.error.toUiText(), kind = MessageKind.Error)
                    }
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

/**
 * Where the list stands once the rows it holds change: rows already stored end a first load's
 * placeholders as a refresh, rows written after an empty answer replace the empty state, and a
 * list emptied elsewhere (a sign-out) shows it.
 */
private fun LoadState.holding(rows: Boolean): LoadState = when {
    rows && this == LoadState.Loading -> LoadState.Refreshing
    rows && this == LoadState.Empty -> LoadState.Success
    !rows && this == LoadState.Success -> LoadState.Empty
    else -> this
}
