package __PKG__.feature.__NAME__.list

import __PKG__.core.common.LoadState
import __PKG__.core.common.result.AppResult
import __PKG__.core.mvi.MessageKind
import __PKG__.core.mvi.MviViewModel
import __PKG__.core.ui.toUiText
import __PKG__.data.__NAME__.api.__P__Repository
import dagger.hilt.android.lifecycle.HiltViewModel
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
            val loading = if (refreshing) LoadState.Refreshing else LoadState.Loading
            setState { copy(loadState = loading) }

            when (val result = repository.items(forceRefresh = refreshing)) {
                is AppResult.Success -> setState {
                    val loaded = if (result.value.isEmpty()) LoadState.Empty else LoadState.Success
                    copy(loadState = loaded, items = result.value)
                }

                // A failed refresh keeps the rows on screen and says so in a message.
                is AppResult.Failure -> if (refreshing && current.items.isNotEmpty()) {
                    setState { copy(loadState = LoadState.Success) }
                    showMessage(text = result.error.toUiText(), kind = MessageKind.Error)
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
