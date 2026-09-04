package com.base.app.core.designsystem.component.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.animation.rememberAppTransitions
import com.base.app.core.designsystem.component.button.AppButton
import com.base.app.core.designsystem.component.button.ButtonSize
import com.base.app.core.designsystem.component.button.ButtonVariant
import com.base.app.core.designsystem.component.feedback.AppCircularProgress
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.appClickable
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.theme.AppTheme

/**
 * A header that opens and closes the block beneath it.
 *
 * Expansion state is the caller's, not the component's. A settings screen that remembers which
 * section was open across a process death cannot do that if the state lives inside the row — and
 * an accordion group where opening one closes the others is impossible without hoisting.
 *
 * The chevron rotates rather than swapping glyphs: 0° to 180° reads as the same object turning,
 * where a chevron-down replaced by a chevron-up reads as a flicker.
 */
@Composable
fun AppAccordion(
    title: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val transitions = rememberAppTransitions()
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = AppTheme.motion.sheet(),
        label = "accordionChevron",
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .appClickable(
                    onClick = { onExpandedChange(!expanded) },
                    enabled = enabled,
                    minTouchTarget = 0.dp,
                )
                .padding(vertical = AppTheme.spacing.md),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                AppText(
                    text = title,
                    style = AppTheme.typography.titleMedium,
                    color = AppTheme.colors.contentPrimary,
                )
                subtitle?.let {
                    AppText(
                        text = it,
                        style = AppTheme.typography.caption,
                        color = AppTheme.colors.contentTertiary,
                    )
                }
            }
            AppIcon(
                imageVector = AppIcons.ChevronDown,
                contentDescription = null,
                tint = AppTheme.colors.contentTertiary,
                modifier = Modifier.rotate(rotation),
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = transitions.expandIn,
            exit = transitions.collapseOut,
        ) {
            Column(
                modifier = Modifier.padding(bottom = AppTheme.spacing.md),
                content = content,
            )
        }
    }
}

/**
 * Where a paged list is up to, as one value rather than three booleans.
 *
 * `isLoading` + `hasMore` + `error` has eight combinations and four meaningful ones, and the
 * invalid ones render as a spinner underneath an error message.
 */
sealed interface LoadMoreState {
    data object Idle : LoadMoreState
    data object Loading : LoadMoreState
    data class Error(val message: String) : LoadMoreState

    /** Everything has been loaded. The footer says so once and then stays quiet. */
    data object Exhausted : LoadMoreState
}

/**
 * The footer of an infinite list.
 *
 * Put it in the last `item { }` of a `LazyColumn`. It is a footer rather than a scroll listener
 * because "the footer became visible" is exactly the signal you want and it needs no threshold
 * arithmetic — a listener firing on "within 3 items of the end" behaves differently for a list of
 * tall cards than for one of thin rows.
 *
 * The error state gives the user a retry rather than retrying silently: a list that keeps
 * re-requesting a failing page burns the battery and the API quota of everyone who leaves the
 * screen open.
 */
@Composable
fun AppLoadMoreFooter(
    state: LoadMoreState,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    exhaustedLabel: String? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(AppTheme.spacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            LoadMoreState.Idle -> {
                // Requesting from composition rather than from a click: reaching the footer *is*
                // the user asking for more.
                LaunchedEffect(Unit) { onLoadMore() }
                AppCircularProgress(size = 20.dp, color = AppTheme.colors.contentTertiary)
            }

            LoadMoreState.Loading ->
                AppCircularProgress(size = 20.dp, color = AppTheme.colors.accent)

            is LoadMoreState.Error -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            ) {
                AppText(
                    text = state.message,
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colors.contentTertiary,
                )
                AppButton(
                    text = "Retry",
                    onClick = onLoadMore,
                    variant = ButtonVariant.Tertiary,
                    size = ButtonSize.Small,
                )
            }

            LoadMoreState.Exhausted -> exhaustedLabel?.let {
                AppText(
                    text = it,
                    style = AppTheme.typography.caption,
                    color = AppTheme.colors.contentTertiary,
                )
            }
        }
    }
}

/**
 * A horizontal pager with a dot indicator.
 *
 * The indicator is here rather than left to the caller because a pager without one gives no hint
 * that there is anything to the right — which is the whole reason a carousel underperforms in
 * testing when the first card fills the screen.
 *
 * The active dot widens into a pill rather than only changing colour, so position is legible
 * without relying on a colour difference.
 *
 * The dots sit over the bottom of the page rather than in a row beneath it. Stacking them in a
 * column would make the pager's height depend on whether the caller's height is bounded: given a
 * fixed height the pager fills it and the dots are pushed off the edge, and given wrap-content the
 * pager measures its tallest page and the dots land correctly. Overlaying removes the difference.
 * A page whose content would collide with them adds bottom padding.
 */
@Composable
fun AppPager(
    pageCount: Int,
    modifier: Modifier = Modifier,
    state: PagerState = rememberPagerState(pageCount = { pageCount }),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    pageSpacing: Dp = AppTheme.spacing.md,
    showIndicator: Boolean = true,
    content: @Composable (page: Int) -> Unit,
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
        HorizontalPager(
            state = state,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = contentPadding,
            pageSpacing = pageSpacing,
        ) { page ->
            content(page)
        }

        if (showIndicator && pageCount > 1) {
            AppPagerIndicator(
                pageCount = pageCount,
                selectedPage = state.currentPage,
                modifier = Modifier.padding(AppTheme.spacing.md),
            )
        }
    }
}

@Composable
fun AppPagerIndicator(
    pageCount: Int,
    selectedPage: Int,
    modifier: Modifier = Modifier,
    activeColor: Color = AppTheme.colors.accent,
    inactiveColor: Color = AppTheme.colors.borderStrong,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { page ->
            val selected = page == selectedPage
            val width by animateDpAsState(
                targetValue = if (selected) DOT_ACTIVE_WIDTH else DOT_SIZE,
                animationSpec = AppTheme.motion.sheet(),
                label = "pagerDotWidth",
            )
            Box(
                modifier = Modifier
                    .width(width)
                    .height(DOT_SIZE)
                    .clip(AppTheme.shapes.pill)
                    .background(if (selected) activeColor else inactiveColor),
            )
        }
    }
}

/** One point on an [AppTimeline]. */
data class TimelineStep(
    val title: String,
    val description: String? = null,
    val timestamp: String? = null,
)

/**
 * A vertical progress trail: order status, a multi-step form, an audit history.
 *
 * The connector between two steps is coloured by the *earlier* of the two, so the line reads as
 * progress flowing forwards and the boundary between done and pending sits exactly where the user
 * expects it. Colouring by the later step puts the transition one node too early — a detail
 * nobody articulates but everybody notices.
 */
@Composable
fun AppTimeline(
    steps: List<TimelineStep>,
    currentIndex: Int,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors

    Column(modifier = modifier.fillMaxWidth()) {
        steps.forEachIndexed { index, step ->
            val done = index < currentIndex
            val active = index == currentIndex
            val nodeColor = when {
                done -> colors.success.content
                active -> colors.accent
                else -> colors.borderStrong
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.width(NODE_COLUMN_WIDTH),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (active) NODE_ACTIVE else NODE_SIZE)
                            .clip(AppTheme.shapes.pill)
                            .background(nodeColor),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (done) {
                            AppIcon(
                                AppIcons.Check,
                                contentDescription = null,
                                tint = Color.White,
                                size = 10.dp,
                            )
                        }
                    }

                    if (index != steps.lastIndex) {
                        Box(
                            modifier = Modifier
                                .width(CONNECTOR_WIDTH)
                                .height(CONNECTOR_HEIGHT)
                                .background(if (done) colors.success.content else colors.border),
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = AppTheme.spacing.md, bottom = AppTheme.spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    AppText(
                        text = step.title,
                        style = if (active) {
                            AppTheme.typography.titleMedium
                        } else {
                            AppTheme.typography.bodyMedium
                        },
                        color = if (index <= currentIndex) {
                            colors.contentPrimary
                        } else {
                            colors.contentTertiary
                        },
                    )
                    step.description?.let {
                        AppText(
                            text = it,
                            style = AppTheme.typography.bodySmall,
                            color = colors.contentTertiary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    step.timestamp?.let {
                        AppText(
                            text = it,
                            style = AppTheme.typography.caption,
                            color = colors.contentTertiary,
                        )
                    }
                }
            }
        }
    }
}

private val DOT_SIZE = 7.dp
private val DOT_ACTIVE_WIDTH = 20.dp
private val NODE_COLUMN_WIDTH = 24.dp
private val NODE_SIZE = 12.dp
private val NODE_ACTIVE = 16.dp
private val CONNECTOR_WIDTH = 2.dp
private val CONNECTOR_HEIGHT = 44.dp
