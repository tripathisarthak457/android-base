package com.example.cmp.feature.feed

import androidx.compose.runtime.Immutable
import com.example.cmp.core.mvi.UiEffect
import com.example.cmp.core.mvi.UiEvent
import com.example.cmp.core.mvi.UiState

@Immutable
internal data class FeedState(
    val expanded: Set<Int> = emptySet(),
) : UiState

internal sealed interface FeedEvent : UiEvent {
    data class ToggleExpanded(val postId: Int) : FeedEvent
}

internal sealed interface FeedEffect : UiEffect
