package com.example.wp04.base.feature.sample

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.wp04.base.core.navigation.AppNavigator
import com.example.wp04.base.core.ui.MviScreen
import com.example.wp04.base.feature.sample.detail.SampleDetailEffect
import com.example.wp04.base.feature.sample.detail.SampleDetailScreen
import com.example.wp04.base.feature.sample.detail.SampleDetailViewModel
import com.example.wp04.base.feature.sample.list.SampleListEffect
import com.example.wp04.base.feature.sample.list.SampleListScreen
import com.example.wp04.base.feature.sample.list.SampleListViewModel

/** The route composables: where a ViewModel, its screen and navigation meet. */
@Composable
fun SampleListRoute(
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
        SampleListScreen(state = state, onEvent = onEvent)
    }
}

@Composable
fun SampleDetailRoute(
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
            }
        },
    ) { state, onEvent ->
        SampleDetailScreen(state = state, onEvent = onEvent)
    }
}
