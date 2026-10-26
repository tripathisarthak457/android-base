package com.example.finalafter.crui.feature.settings.licenses

import com.example.finalafter.crui.core.common.LoadState
import com.example.finalafter.crui.core.mvi.UiEffect
import com.example.finalafter.crui.core.mvi.UiEvent
import com.example.finalafter.crui.core.mvi.UiState

/**
 * One dependency, as the build recorded it. The licence is a list because a library can offer a
 * choice of them, and showing all of them is more honest than showing the first.
 */
internal data class LicensedArtifact(
    val name: String,
    val coordinates: String,
    val licences: List<String>,
)

internal data class LicensesState(
    val loadState: LoadState = LoadState.Idle,
    val artifacts: List<LicensedArtifact> = emptyList(),
) : UiState

internal sealed interface LicensesEvent : UiEvent {
    data object BackClicked : LicensesEvent
}

internal sealed interface LicensesEffect : UiEffect {
    data object NavigateBack : LicensesEffect
}
