package com.base.app.feature.sample.detail

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.base.app.core.navigation.AppNavigator
import com.base.app.core.ui.MviScreen
import com.base.app.feature.sample.api.SampleDetailKey

@Composable
internal fun SampleDetailRoute(
    itemId: Int,
    navigator: AppNavigator,
    viewModel: SampleDetailViewModel =
        hiltViewModel<SampleDetailViewModel, SampleDetailViewModel.Factory>(
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
