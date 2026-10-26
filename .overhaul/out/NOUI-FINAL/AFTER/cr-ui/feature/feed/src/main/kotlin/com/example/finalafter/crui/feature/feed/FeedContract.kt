package com.example.finalafter.crui.feature.feed

import androidx.compose.runtime.Immutable
import com.example.finalafter.crui.core.mvi.UiEffect
import com.example.finalafter.crui.core.mvi.UiEvent
import com.example.finalafter.crui.core.mvi.UiState

@Immutable
internal data class FeedState(
    val expanded: Set<Int> = emptySet(),
) : UiState

internal sealed interface FeedEvent : UiEvent {
    data class ToggleExpanded(val postId: Int) : FeedEvent
}

internal sealed interface FeedEffect : UiEffect
