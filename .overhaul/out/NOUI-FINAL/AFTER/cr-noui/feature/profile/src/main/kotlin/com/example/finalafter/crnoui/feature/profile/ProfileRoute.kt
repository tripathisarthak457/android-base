package com.example.finalafter.crnoui.feature.profile

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.finalafter.crnoui.core.ui.MviScreen
import com.example.finalafter.crnoui.core.ui.PlaceholderScreen

@Composable
internal fun ProfileRoute(viewModel: ProfileViewModel = hiltViewModel()) {
    MviScreen(viewModel = viewModel) { _, _ ->
        PlaceholderScreen(title = R.string.profile_title)
    }
}
