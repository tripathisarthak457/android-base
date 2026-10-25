package com.base.app.feature.settings.licenses

import com.base.app.core.common.LoadState
import com.base.app.core.mvi.UiEffect
import com.base.app.core.mvi.UiEvent
import com.base.app.core.mvi.UiState

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
