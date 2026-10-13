package __PKG__.feature.__NAME__.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import __PKG__.core.navigation.AppNavigator
import __PKG__.core.ui.MviScreen
import __PKG__.feature.__NAME__.api.__P__DetailKey

@Composable
internal fun __P__ListRoute(
    navigator: AppNavigator,
    viewModel: __P__ListViewModel = hiltViewModel(),
) {
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
    ) { state, onEvent ->
        __P__ListScreen(state = state, onEvent = onEvent)
    }
}
