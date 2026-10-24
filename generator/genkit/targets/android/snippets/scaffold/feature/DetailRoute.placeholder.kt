package __PKG__.feature.__NAME__.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import __PKG__.core.navigation.AppNavigator
import __PKG__.core.ui.MviScreen
import __PKG__.core.ui.PlaceholderScreen
import __PKG__.feature.__NAME__.R

/**
 * The detail, wired to its ViewModel as a finished screen is: it loads the item its key names and
 * follows its effects. What it draws is a placeholder until the project's design system has
 * components to build the screen from.
 */
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
    LaunchedEffect(Unit) { viewModel.onEvent(__P__DetailEvent.Appeared) }

    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                __P__DetailEffect.NavigateBack -> navigator.navigateUp()
            }
        },
    ) { _, _ ->
        PlaceholderScreen(title = R.string.__NAME___title)
    }
}
