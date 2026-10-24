package com.example.deskstandard.feature.sample.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.deskstandard.core.navigation.AppNavigator
import com.example.deskstandard.core.ui.MviScreen
import com.example.deskstandard.feature.sample.api.SampleDetailKey

@Composable
internal fun SampleDetailRoute(
    itemId: Int,
    navigator: AppNavigator,
    viewModel: SampleDetailViewModel = hiltViewModel<SampleDetailViewModel, SampleDetailViewModel.Factory>(
        creationCallback = { factory -> factory.create(itemId) },
    ),
) {
    LaunchedEffect(Unit) { viewModel.onEvent(SampleDetailEvent.Appeared) }

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
