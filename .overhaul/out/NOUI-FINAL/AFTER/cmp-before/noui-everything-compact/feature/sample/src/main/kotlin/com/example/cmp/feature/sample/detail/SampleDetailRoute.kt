package com.example.cmp.feature.sample.detail

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.cmp.core.navigation.AppNavigator
import com.example.cmp.core.ui.MviScreen
import com.example.cmp.core.ui.PlaceholderScreen
import com.example.cmp.feature.sample.R
import com.example.cmp.feature.sample.api.SampleDetailKey

/**
 * Where the ViewModel and navigation meet. The item loads, with its related items, as the finished
 * screen will; what it draws is a placeholder until the design system has the components to build
 * that screen from.
 */
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
    ) { _, _ ->
        PlaceholderScreen(title = R.string.sample_detail_title)
    }
}
