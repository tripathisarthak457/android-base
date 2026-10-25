package com.base.app.feature.profile

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.base.app.core.ui.MviScreen
// <opt:!ui_components>import com.base.app.core.ui.PlaceholderScreen

@Composable
internal fun ProfileRoute(viewModel: ProfileViewModel = hiltViewModel()) {
    // <opt:ui_components>
    MviScreen(viewModel = viewModel) { state, onEvent ->
        ProfileScreen(state = state, name = viewModel.name, bio = viewModel.bio, onEvent = onEvent)
    }
    // </opt:ui_components>
    // <opt-else:ui_components>
    //~ MviScreen(viewModel = viewModel) { _, _ ->
    //~     PlaceholderScreen(title = R.string.profile_title)
    //~ }
    // </opt-else:ui_components>
}
