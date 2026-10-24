package com.example.deskstandard.feature.sample.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.deskstandard.core.navigation.AppNavigator
import com.example.deskstandard.core.ui.MviScreen
import com.example.deskstandard.feature.sample.api.SampleDetailKey

/** Where the ViewModel, its screen and navigation meet. */
@Composable
internal fun SampleListRoute(
    navigator: AppNavigator,
    viewModel: SampleListViewModel = hiltViewModel(),
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
