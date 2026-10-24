package __PKG__.feature.__NAME__.list

import __PKG__.core.common.LoadState
import __PKG__.core.common.result.AppResult
import __PKG__.core.mvi.MessageKind
import __PKG__.core.mvi.MviViewModel
import __PKG__.core.ui.toUiText
import __PKG__.data.__NAME__.api.__P__Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@HiltViewModel
__FIT__
internal class __P__ListViewModel @Inject constructor(private val repository: __P__Repository) :
    MviViewModel<__P__ListState, __P__ListEvent, __P__ListEffect>(
        initial = __P__ListState(),
    ) {
__ELSE__
internal class __P__ListViewModel @Inject constructor(
    private val repository: __P__Repository,
) : MviViewModel<__P__ListState, __P__ListEvent, __P__ListEffect>(
    initial = __P__ListState(),
) {
__ELSE__
internal class __P__ListViewModel @Inject constructor(
    private val repository: __P__Repository,
) : MviViewModel<
    __P__ListState,
    __P__ListEvent,
    __P__ListEffect,
    >(
    initial = __P__ListState(),
) {
__END_FIT__

    init {
        // Every write to the list reaches the screen, not only the ones a refresh makes.
        observe(repository.items) { items ->
            copy(items = items, loadState = loadState.holding(items.isNotEmpty()))
        }
    }

    override fun handleEvent(event: __P__ListEvent) {
        when (event) {
            __P__ListEvent.Appeared -> {
                if (current.loadState == LoadState.Idle) load(refreshing = false)
            }

            __P__ListEvent.Refresh -> load(refreshing = true)

            __P__ListEvent.Retry -> load(refreshing = false)

            is __P__ListEvent.ItemClicked -> {
                emit(__P__ListEffect.OpenDetail(event.id))
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
