package com.example.finalafter.crnoui3.feature.feed

import androidx.compose.runtime.Immutable
import com.example.finalafter.crnoui3.core.mvi.UiEffect
import com.example.finalafter.crnoui3.core.mvi.UiEvent
import com.example.finalafter.crnoui3.core.mvi.UiState

@Immutable
internal data class FeedState(
    val expanded: Set<Int> = emptySet(),
) : UiState

internal sealed interface FeedEvent : UiEvent {
    data class ToggleExpanded(val postId: Int) : FeedEvent
}

internal sealed interface FeedEffect : UiEffect
