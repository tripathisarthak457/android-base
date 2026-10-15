package com.base.app.core.designsystem.component.navigation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.R
import com.base.app.core.designsystem.foundation.interaction.AppHaptic
import com.base.app.core.designsystem.foundation.interaction.rememberAppHaptics
import com.base.app.core.designsystem.foundation.scroll.AppScrollDefaults
import com.base.app.core.designsystem.theme.AppTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** A horizontal pager with a page control under it, driven by the same state. */
@Composable
fun AppPager(
    pageCount: Int,
    modifier: Modifier = Modifier,
    state: PagerState = rememberPagerState(pageCount = { pageCount }),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    pageSpacing: Dp = AppTheme.spacing.md,
    showPageControl: Boolean = true,
    content: @Composable (page: Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
        HorizontalPager(
            state = state,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = contentPadding,
            flingBehavior = AppScrollDefaults.pagerFlingBehavior(state),
            pageSpacing = pageSpacing,
        ) { page -> content(page) }

        if (showPageControl && pageCount > 1) {
            AppPageControl(
                pageCount = pageCount,
                selected = { state.currentPage },
                onSelect = { page -> scope.launch { state.animateScrollToPage(page) } },
                modifier = Modifier.padding(AppTheme.spacing.md),
            )
        }
    }
}

/**
 * iOS's page control: a dot per page, the current one solid. Tap a side to step one page that way, or
 * drag along it to scrub. A screen reader hears "Page 2 of 5" and can adjust it; that description is
 * the only part recomposed when the page changes, while the dots are redrawn from [selected].
 */
@Composable
fun AppPageControl(
    pageCount: Int,
    selected: () -> Int,
    modifier: Modifier = Modifier,
    onSelect: ((Int) -> Unit)? = null,
) {
    val colors = AppTheme.colors
    val haptics = rememberAppHaptics()
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val select by rememberUpdatedState(onSelect)
    val description = stringResource(R.string.designsystem_page_of, selected() + 1, pageCount)

    fun pageAt(x: Float, width: Float): Int {
        val f = (x / width).coerceIn(0f, 0.999f)
        val page = (f * pageCount).toInt()
        return if (rtl) pageCount - 1 - page else page
    }

    fun go(page: Int) {
        val target = page.coerceIn(0, pageCount - 1)
        if (target == selected()) return
        haptics.perform(AppHaptic.Selection)
        select?.invoke(target)
    }

    Canvas(
        modifier = modifier
            .width(DotStep * pageCount + 16.dp)
            .height(MinHeight)
            .semantics {
                stateDescription = description
                progressBarRangeInfo = ProgressBarRangeInfo(selected().toFloat(), 0f..(pageCount - 1).toFloat(), pageCount - 2)
                if (onSelect != null) {
                    setProgress { target ->
                        go(target.roundToInt())
                        true
                    }
                }
            }
            .then(
                if (onSelect != null) {
                    Modifier
                        .pointerInput(pageCount, rtl) {
                            detectTapGestures { offset ->
                                val forward = (offset.x > size.width / 2f) != rtl
                                go(selected() + if (forward) 1 else -1)
                            }
                        }
                        .pointerInput(pageCount, rtl) {
                            detectHorizontalDragGestures { change, _ -> go(pageAt(change.position.x, size.width.toFloat())) }
                        }
                } else {
                    Modifier
                },
            ),
    ) {
        val step = DotStep.toPx()
        val radius = DotSize.toPx() / 2f
        val start = (size.width - step * pageCount) / 2f + step / 2f
        val current = selected()
        for (page in 0 until pageCount) {
            val index = if (rtl) pageCount - 1 - page else page
            drawCircle(
                color = if (index == current) colors.contentPrimary else colors.contentPrimary.copy(alpha = 0.3f),
                radius = radius,
                center = Offset(start + step * page, size.height / 2f),
            )
        }
    }
}

private val DotSize = 7.dp
private val DotStep = 16.dp
private val MinHeight = 28.dp
