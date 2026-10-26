package com.example.finalafter.crui2.feature.profile

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.finalafter.crui2.core.ui.MviScreen

@Composable
internal fun ProfileRoute(viewModel: ProfileViewModel = hiltViewModel()) {
    MviScreen(viewModel = viewModel) { state, onEvent ->
        ProfileScreen(state = state, name = viewModel.name, bio = viewModel.bio, onEvent = onEvent)
    }
}
