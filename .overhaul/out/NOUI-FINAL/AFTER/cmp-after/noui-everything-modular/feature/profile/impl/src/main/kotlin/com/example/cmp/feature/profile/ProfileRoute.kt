package com.example.cmp.feature.profile

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.cmp.core.ui.MviScreen
import com.example.cmp.core.ui.PlaceholderScreen

@Composable
internal fun ProfileRoute(viewModel: ProfileViewModel = hiltViewModel()) {
    MviScreen(viewModel = viewModel) { _, _ ->
        PlaceholderScreen(title = R.string.profile_title)
    }
}
