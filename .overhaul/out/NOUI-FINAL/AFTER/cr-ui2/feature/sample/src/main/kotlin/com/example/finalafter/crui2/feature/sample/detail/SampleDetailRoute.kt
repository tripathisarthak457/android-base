package com.example.finalafter.crui2.feature.sample.detail

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.finalafter.crui2.core.navigation.AppNavigator
import com.example.finalafter.crui2.core.ui.MviScreen
import com.example.finalafter.crui2.feature.sample.api.SampleDetailKey

@Composable
internal fun SampleDetailRoute(
    itemId: Int,
    navigator: AppNavigator,
    viewModel: SampleDetailViewModel = hiltViewModel<SampleDetailViewModel, SampleDetailViewModel.Factory>(
        creationCallback = { factory -> factory.create(itemId) },
    ),
) {
    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                SampleDetailEffect.NavigateBack -> navigator.navigateUp()
                is SampleDetailEffect.OpenDetail -> navigator.navigate(SampleDetailKey(effect.id))
            }
        },
    ) { state, onEvent ->
        SampleDetailScreen(state = state, onEvent = onEvent)
    }
}
