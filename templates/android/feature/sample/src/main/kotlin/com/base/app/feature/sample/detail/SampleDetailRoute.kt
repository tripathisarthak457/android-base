package com.base.app.feature.sample.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.base.app.core.navigation.AppNavigator
import com.base.app.core.navigation.LocalNavigationSettled
import com.base.app.core.ui.MviScreen
import com.base.app.feature.sample.api.SampleDetailKey
import kotlinx.coroutines.flow.first

@Composable
internal fun SampleDetailRoute(
    itemId: Int,
    navigator: AppNavigator,
    viewModel: SampleDetailViewModel = hiltViewModel<SampleDetailViewModel, SampleDetailViewModel.Factory>(
        creationCallback = { factory -> factory.create(itemId) },
    ),
) {
    LaunchedEffect(Unit) { viewModel.onEvent(SampleDetailEvent.Appeared) }
    // The related rows wait for the push to settle, so they are not composed in a frame of the slide.
    val settled = LocalNavigationSettled.current
    LaunchedEffect(settled) {
        snapshotFlow { settled() }.first { it }
        viewModel.onEvent(SampleDetailEvent.LoadRelated)
    }

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
