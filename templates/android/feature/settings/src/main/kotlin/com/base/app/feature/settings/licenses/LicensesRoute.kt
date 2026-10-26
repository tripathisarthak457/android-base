package com.base.app.feature.settings.licenses

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.base.app.core.navigation.AppNavigator
import com.base.app.core.ui.MviScreen
// <opt:!ui_components>import com.base.app.core.ui.PlaceholderScreen
// <opt:!ui_components>import com.base.app.feature.settings.R

@Composable
internal fun LicensesRoute(
    navigator: AppNavigator,
    viewModel: LicensesViewModel = hiltViewModel(),
) {
    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                LicensesEffect.NavigateBack -> navigator.navigateUp()
            }
        },
        // <opt:ui_components>
    ) { state, onEvent ->
        LicensesScreen(state = state, onEvent = onEvent)
    }
    // </opt:ui_components>
    // <opt-else:ui_components>
    //~ ) { _, _ ->
    //~     PlaceholderScreen(title = R.string.settings_open_source_licences)
    //~ }
    // </opt-else:ui_components>
}
