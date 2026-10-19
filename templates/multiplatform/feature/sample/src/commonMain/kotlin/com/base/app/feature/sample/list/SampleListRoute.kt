package com.base.app.feature.sample.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.base.app.core.navigation.AppNavigator
import com.base.app.core.ui.MviScreen
import com.base.app.feature.sample.api.SampleDetailKey
import org.koin.compose.viewmodel.koinViewModel

/** Where the ViewModel, its screen and navigation meet. */
@Composable
internal fun SampleListRoute(
    navigator: AppNavigator,
    viewModel: SampleListViewModel = koinViewModel(),
) {
    LaunchedEffect(Unit) { viewModel.onEvent(SampleListEvent.Appeared) }

    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                is SampleListEffect.OpenDetail -> navigator.navigate(SampleDetailKey(effect.id))
            }
        },
    ) { state, onEvent ->
        SampleListScreen(state = state, query = viewModel.query, onEvent = onEvent)
    }
}
