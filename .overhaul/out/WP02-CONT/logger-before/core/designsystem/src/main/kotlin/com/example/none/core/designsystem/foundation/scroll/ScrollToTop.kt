package com.example.none.core.designsystem.foundation.scroll

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState

/**
 * Back to the top, as iOS goes there when the status bar is tapped or the active tab tapped again.
 * A list more than two screens down first jumps to two screens from the top and glides only that
 * last stretch, so the trip is short however far down it was, and composes only the rows of those
 * two screens on the way.
 */
suspend fun LazyListState.animateScrollToTop() {
    val screen = layoutInfo.visibleItemsInfo.size.coerceAtLeast(1)
    val approach = screen * ApproachScreens
    if (firstVisibleItemIndex > approach) scrollToItem(approach)
    animateScrollToItem(0)
}

/** [LazyListState.animateScrollToTop] for a scrolling column. */
suspend fun ScrollState.animateScrollToTop() {
    val approach = viewportSize * ApproachScreens
    if (approach > 0 && value > approach) scrollTo(approach)
    animateScrollTo(0)
}

private const val ApproachScreens = 2
