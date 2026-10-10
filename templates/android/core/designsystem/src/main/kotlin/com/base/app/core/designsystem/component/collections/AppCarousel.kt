package com.base.app.core.designsystem.component.collections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.base.app.core.designsystem.component.navigation.AppPageControl
import com.base.app.core.designsystem.foundation.scroll.AppScrollDefaults
import com.base.app.core.designsystem.theme.AppTheme
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Cards that snap one at a time with the next one peeking in, and a page control beneath. Pages
 * either side of the current one shrink slightly; the effect is applied to each page's layer, so a
 * swipe moves and scales pages without recomposing them.
 */
@Composable
fun AppCarousel(
    pageCount: Int,
    modifier: Modifier = Modifier,
    state: PagerState = rememberPagerState { pageCount },
    peek: Dp = 32.dp,
    pageSpacing: Dp = AppTheme.spacing.md,
    showPageControl: Boolean = true,
    content: @Composable (page: Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm), horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(
            state = state,
            modifier = Modifier.fillMaxWidth(),
            flingBehavior = AppScrollDefaults.pagerFlingBehavior(state),
            contentPadding = PaddingValues(horizontal = AppTheme.spacing.gutter + peek),
            pageSpacing = pageSpacing,
            key = { it },
        ) { page ->
            Box(
                Modifier.graphicsLayer {
                    val distance = abs((state.currentPage - page) + state.currentPageOffsetFraction).coerceIn(0f, 1f)
                    val scale = lerp(1f, SideScale, distance)
                    scaleX = scale
                    scaleY = scale
                },
            ) {
                content(page)
            }
        }
        if (showPageControl && pageCount > 1) {
            AppPageControl(
                pageCount = pageCount,
                selected = { state.currentPage },
                onSelect = { page -> scope.launch { state.animateScrollToPage(page) } },
            )
        }
    }
}

private const val SideScale = 0.92f
