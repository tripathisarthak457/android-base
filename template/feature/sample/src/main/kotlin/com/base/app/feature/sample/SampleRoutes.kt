package com.base.app.feature.sample

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.base.app.core.navigation.AppNavigator
import com.base.app.core.ui.MviScreen
import com.base.app.feature.sample.detail.SampleDetailEffect
import com.base.app.feature.sample.detail.SampleDetailEvent
import com.base.app.feature.sample.detail.SampleDetailScreen
import com.base.app.feature.sample.detail.SampleDetailViewModel
import com.base.app.feature.sample.list.SampleListEffect
import com.base.app.feature.sample.list.SampleListScreen
import com.base.app.feature.sample.list.SampleListViewModel

/**
 * The route composables: where a ViewModel, its screen and navigation meet.
 *
 * Kept apart from the screens so the screens stay free of Hilt and of the navigator, which is
 * what lets them be previewed and tested as pure functions of their state.
 */
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
    viewModel: SampleDetailViewModel = hiltViewModel(),
) {
    // Keyed on the id: returning to this screen for a different item re-fires the load, and
    // returning for the same one does not, because the ViewModel short-circuits a repeat.
    LaunchedEffect(itemId) { viewModel.onEvent(SampleDetailEvent.Load(itemId)) }

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
