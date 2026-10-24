package __PKG__.feature.__NAME__.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import __PKG__.core.navigation.AppNavigator
import __PKG__.core.ui.MviScreen
import __PKG__.core.ui.PlaceholderScreen
import __PKG__.feature.__NAME__.R
import __PKG__.feature.__NAME__.api.__P__DetailKey

/**
 * The list, wired to its ViewModel as a finished screen is: it loads, receives its state and
 * follows its effects. What it draws is a placeholder until the project's design system has
 * components to build the screen from.
 */
@Composable
__FIT__
internal fun __P__ListRoute(navigator: AppNavigator, viewModel: __P__ListViewModel = hiltViewModel()) {
__ELSE__
internal fun __P__ListRoute(
    navigator: AppNavigator,
    viewModel: __P__ListViewModel = hiltViewModel(),
) {
__END_FIT__
    LaunchedEffect(Unit) { viewModel.onEvent(__P__ListEvent.Appeared) }

    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                is __P__ListEffect.OpenDetail -> {
                    navigator.navigate(__P__DetailKey(effect.id))
                }
            }
        },
    ) { _, _ ->
        PlaceholderScreen(title = R.string.__NAME___title)
    }
}
