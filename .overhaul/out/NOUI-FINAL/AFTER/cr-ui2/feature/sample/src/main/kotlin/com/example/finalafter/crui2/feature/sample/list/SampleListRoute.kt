package com.example.finalafter.crui2.feature.sample.list

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.finalafter.crui2.core.navigation.AppNavigator
import com.example.finalafter.crui2.core.ui.MviScreen
import com.example.finalafter.crui2.feature.sample.api.SampleDetailKey

/** Where the ViewModel, its screen and navigation meet. */
@Composable
internal fun SampleListRoute(
    navigator: AppNavigator,
    viewModel: SampleListViewModel = hiltViewModel(),
) {
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
