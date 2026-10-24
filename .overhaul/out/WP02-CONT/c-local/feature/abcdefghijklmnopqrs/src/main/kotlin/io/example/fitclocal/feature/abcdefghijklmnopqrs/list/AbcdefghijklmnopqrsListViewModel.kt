package io.example.fitclocal.feature.abcdefghijklmnopqrs.list

import dagger.hilt.android.lifecycle.HiltViewModel
import io.example.fitclocal.core.common.LoadState
import io.example.fitclocal.core.common.result.AppResult
import io.example.fitclocal.core.mvi.MessageKind
import io.example.fitclocal.core.mvi.MviViewModel
import io.example.fitclocal.core.ui.toUiText
import io.example.fitclocal.data.abcdefghijklmnopqrs.api.AbcdefghijklmnopqrsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

@HiltViewModel
internal class AbcdefghijklmnopqrsListViewModel @Inject constructor(
    private val repository: AbcdefghijklmnopqrsRepository,
) : MviViewModel<
    AbcdefghijklmnopqrsListState,
    AbcdefghijklmnopqrsListEvent,
    AbcdefghijklmnopqrsListEffect,
    >(
    initial = AbcdefghijklmnopqrsListState(),
) {

    init {
        // Every write to the list reaches the screen, not only the ones a refresh makes.
        observe(repository.items) { items ->
            copy(items = items, loadState = loadState.holding(items.isNotEmpty()))
        }
    }

    override fun handleEvent(event: AbcdefghijklmnopqrsListEvent) {
        when (event) {
            AbcdefghijklmnopqrsListEvent.Appeared -> {
                if (current.loadState == LoadState.Idle) load(refreshing = false)
            }

            AbcdefghijklmnopqrsListEvent.Refresh -> load(refreshing = true)

            AbcdefghijklmnopqrsListEvent.Retry -> load(refreshing = false)

            is AbcdefghijklmnopqrsListEvent.ItemClicked -> {
                emit(AbcdefghijklmnopqrsListEffect.OpenDetail(event.id))
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
 * placeholders as a refresh, rows written after an empty answer or a failed first load replace
 * that state, and a list emptied elsewhere (a sign-out) shows it.
 */
private fun LoadState.holding(rows: Boolean): LoadState = when {
    rows && this == LoadState.Loading -> LoadState.Refreshing
    rows && (this == LoadState.Empty || this is LoadState.Error) -> LoadState.Success
    !rows && this == LoadState.Success -> LoadState.Empty
    else -> this
}
