package com.example.finalafter.crui.core.designsystem.component.collections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.example.finalafter.crui.core.designsystem.component.navigation.AppPageControl
import com.example.finalafter.crui.core.designsystem.component.text.AppText
import com.example.finalafter.crui.core.designsystem.foundation.content.AppSurface
import com.example.finalafter.crui.core.designsystem.foundation.scroll.AppScrollDefaults
import com.example.finalafter.crui.core.designsystem.preview.AppComponentPreviews
import com.example.finalafter.crui.core.designsystem.theme.AppTheme
import kotlin.math.abs
import kotlinx.coroutines.launch

/**
 * Cards that snap one at a time with the next one peeking in, and a page control beneath. Pages
 * either side of the current one shrink slightly; the effect is applied to each page's layer, so a
 * swipe moves and scales pages without recomposing them. Given a height, the cards take what the
 * page control leaves, so the dots never sit on a card.
 */
@Composable
fun AppCarousel(
    pageCount: Int,
    modifier: Modifier = Modifier,
    state: PagerState = rememberPagerState { pageCount },
    peek: Dp = AppCarouselDefaults.Peek,
    pageSpacing: Dp = AppTheme.spacing.md,
    showPageControl: Boolean = true,
    content: @Composable (page: Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val gap = AppTheme.spacing.sm
    Layout(
        content = {
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
        },
        modifier = modifier.fillMaxWidth(),
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val control = measurables.getOrNull(1)?.measure(loose)
        val reserved = control?.let { it.height + gap.roundToPx() } ?: 0
        val pagerHeight = if (constraints.hasBoundedHeight) (constraints.maxHeight - reserved).coerceAtLeast(0) else Constraints.Infinity
        val pager = measurables[0].measure(loose.copy(maxHeight = pagerHeight))
        val width = constraints.constrainWidth(pager.width)
        val height = constraints.constrainHeight(pager.height + reserved)
        layout(width, height) {
            pager.placeRelative(0, 0)
            control?.placeRelative((width - control.width) / 2, height - control.height)
        }
    }
}

private const val SideScale = 0.92f

/** How much of the next slide shows. */
object AppCarouselDefaults {
    val Peek = 32.dp
}

@AppComponentPreviews
@Composable
private fun AppCarouselPreview() {
    Column(modifier = Modifier.background(AppTheme.colors.background).padding(vertical = AppTheme.spacing.lg)) {
        AppCarousel(pageCount = 3) { page ->
            AppSurface(
                modifier = Modifier.fillMaxWidth().height(AppTheme.sizes.rowHeight * 3),
                color = AppTheme.colors.accentSubtle,
                shape = AppTheme.shapes.lg,
            ) {
                AppText(text = "Slide ${page + 1}", modifier = Modifier.padding(AppTheme.spacing.lg))
            }
        }
    }
}
