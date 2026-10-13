package __PKG__.feature.__NAME__.detail

import __PKG__.core.common.LoadState
import __PKG__.core.common.result.AppResult
import __PKG__.core.mvi.MviViewModel
import __PKG__.data.__NAME__.api.__P__Repository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

/** The id arrives through assisted injection, at construction, from the navigation key. */
@HiltViewModel(assistedFactory = __P__DetailViewModel.Factory::class)
internal class __P__DetailViewModel @AssistedInject constructor(
    private val repository: __P__Repository,
    @Assisted private val id: String,
__FIT__
) : MviViewModel<__P__DetailState, __P__DetailEvent, __P__DetailEffect>(
    initial = __P__DetailState(),
) {
__ELSE__
) : MviViewModel<
    __P__DetailState,
    __P__DetailEvent,
    __P__DetailEffect,
    >(
    initial = __P__DetailState(),
) {
__END_FIT__

    @AssistedFactory
    interface Factory {
        fun create(id: String): __P__DetailViewModel
    }

    override fun handleEvent(event: __P__DetailEvent) {
        when (event) {
            __P__DetailEvent.Appeared -> {
                if (current.loadState == LoadState.Idle) load()
            }

            __P__DetailEvent.Retry -> load()

            __P__DetailEvent.BackClicked -> {
                emit(__P__DetailEffect.NavigateBack)
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
