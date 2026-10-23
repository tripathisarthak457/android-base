package __PKG__.feature.__NAME__.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import __PKG__.core.navigation.AppNavigator
import __PKG__.core.ui.MviScreen

@Composable
internal fun __P__DetailRoute(
    id: String,
    navigator: AppNavigator,
    viewModel: __P__DetailViewModel =
__FIT__
        hiltViewModel<__P__DetailViewModel, __P__DetailViewModel.Factory>(
            creationCallback = { factory -> factory.create(id) },
        ),
__ELSE__
        hiltViewModel<
            __P__DetailViewModel,
            __P__DetailViewModel.Factory,
            >(
            creationCallback = { factory -> factory.create(id) },
        ),
__END_FIT__
) {
    // Once, when the screen enters composition. Sending an event suspends nothing, so it needs no
    // coroutine of its own.
    SideEffect(Unit) { viewModel.onEvent(__P__DetailEvent.Appeared) }

    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                __P__DetailEffect.NavigateBack -> navigator.navigateUp()
            }
        },
    ) { state, onEvent ->
        __P__DetailScreen(state = state, onEvent = onEvent)
    }
}
