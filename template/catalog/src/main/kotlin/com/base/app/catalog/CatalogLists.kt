package com.base.app.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.animation.appAnimateItem
import com.base.app.core.designsystem.component.button.AppButton
import com.base.app.core.designsystem.component.button.ButtonSize
import com.base.app.core.designsystem.component.button.ButtonVariant
import com.base.app.core.designsystem.component.container.AppCard
import com.base.app.core.designsystem.component.container.AppListItem
import com.base.app.core.designsystem.component.list.AppAccordion
import com.base.app.core.designsystem.component.list.AppLoadMoreFooter
import com.base.app.core.designsystem.component.list.AppPager
import com.base.app.core.designsystem.component.list.AppSwipeToAction
import com.base.app.core.designsystem.component.list.AppTimeline
import com.base.app.core.designsystem.component.list.LoadMoreState
import com.base.app.core.designsystem.component.list.SwipeAction
import com.base.app.core.designsystem.component.list.TimelineStep
import com.base.app.core.designsystem.component.refresh.AppPullToRefresh
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.theme.AppTheme
import kotlinx.coroutines.delay

/**
 * Lists, and the gestures that act on them.
 *
 * The scrolling examples are given an explicit height. A `LazyColumn` measured inside a scrolling
 * parent gets an infinite height constraint and crashes — showing it working at a fixed height is
 * both the honest demo and a reminder of the rule.
 */
@Composable
fun ListsSection() {
    CatalogGroup(
        title = "Swipe to act",
        caption = "Swipe a row left. A hard swipe fires a destructive action outright.",
    ) {
        SwipeDemo()
    }

    CatalogGroup(title = "Accordion", caption = "Opens in place; the chevron turns with it.") {
        AccordionDemo()
    }

    CatalogGroup(
        title = "Pull to refresh",
        caption = "Drag the list down. The indicator resists past the threshold.",
    ) {
        PullToRefreshDemo()
    }

    CatalogGroup(
        title = "Infinite list",
        caption = "The footer asks for the next page when it scrolls into view.",
    ) {
        LoadMoreDemo()
    }

    CatalogGroup(title = "Reordering", caption = "Rows animate between positions, keyed by id.") {
        AnimatedListDemo()
    }

    CatalogGroup(title = "Pager", caption = "The active dot widens rather than only recolouring.") {
        AppPager(
            pageCount = PAGER_PAGES,
            modifier = Modifier.height(140.dp),
        ) { page ->
            AppCard(modifier = Modifier.fillMaxSize()) {
                AppText(
                    text = "Page ${page + 1}",
                    style = AppTheme.typography.headingSmall,
                    color = AppTheme.colors.contentPrimary,
                )
            }
        }
    }

    CatalogGroup(title = "Timeline", caption = "The connector is coloured by the earlier step.") {
        AppTimeline(
            steps = listOf(
                TimelineStep("Ordered", "Payment confirmed", "09:12"),
                TimelineStep("Packed", "Leaving the warehouse", "11:40"),
                TimelineStep("Out for delivery", timestamp = "Today"),
                TimelineStep("Delivered"),
            ),
            currentIndex = 2,
        )
    }
}

@Composable
private fun SwipeDemo() {
    var rows by remember { mutableStateOf(List(SWIPE_ROWS) { "Message ${it + 1}" }) }
    val colors = AppTheme.colors

    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
        rows.forEach { row ->
            AppSwipeToAction(
                actions = listOf(
                    SwipeAction(
                        label = "Archive",
                        icon = AppIcons.Check,
                        onClick = { rows = rows - row },
                        containerColor = colors.accent,
                    ),
                    SwipeAction(
                        label = "Delete",
                        icon = AppIcons.Trash,
                        onClick = { rows = rows - row },
                        containerColor = colors.danger.content,
                        isDestructive = true,
                    ),
                ),
                resetKey = rows,
            ) {
                AppListItem(title = row, supporting = "Swipe me")
            }
        }

        if (rows.size < SWIPE_ROWS) {
            AppButton(
                text = "Put them back",
                onClick = { rows = List(SWIPE_ROWS) { "Message ${it + 1}" } },
                variant = ButtonVariant.Ghost,
                size = ButtonSize.Small,
            )
        }
    }
}

@Composable
private fun AccordionDemo() {
    var openIndex by remember { mutableStateOf<Int?>(0) }

    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
        AccordionEntries.forEachIndexed { index, entry ->
            AppAccordion(
                title = entry.first,
                subtitle = if (openIndex == index) null else "Tap to open",
                expanded = openIndex == index,
                onExpandedChange = { open -> openIndex = if (open) index else null },
            ) {
                AppText(
                    text = entry.second,
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colors.contentSecondary,
                )
            }
        }
    }
}

@Composable
private fun PullToRefreshDemo() {
    var refreshing by remember { mutableStateOf(false) }
    var generation by remember { mutableIntStateOf(1) }

    if (refreshing) {
        LaunchedEffect(generation) {
            delay(REFRESH_MILLIS)
            generation++
            refreshing = false
        }
    }

    AppPullToRefresh(
        isRefreshing = refreshing,
        onRefresh = { refreshing = true },
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(PULL_ROWS) { index ->
                AppListItem(
                    title = "Row ${index + 1}",
                    supporting = "Load $generation",
                )
            }
        }
    }
}

@Composable
private fun LoadMoreDemo() {
    var count by remember { mutableIntStateOf(PAGE_SIZE) }
    var state by remember { mutableStateOf<LoadMoreState>(LoadMoreState.Idle) }

    if (state is LoadMoreState.Loading) {
        LaunchedEffect(count) {
            delay(LOAD_MILLIS)
            count += PAGE_SIZE
            state = if (count >= MAX_ROWS) LoadMoreState.Exhausted else LoadMoreState.Idle
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp),
    ) {
        items(count) { index -> AppListItem(title = "Item ${index + 1}") }

        item {
            AppLoadMoreFooter(
                state = state,
                onLoadMore = { state = LoadMoreState.Loading },
                exhaustedLabel = "That is everything",
            )
        }
    }
}

@Composable
private fun AnimatedListDemo() {
    var rows by remember { mutableStateOf((1..ANIMATED_ROWS).toList()) }

    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            ) {
                // The key is what makes the animation possible: without one, Compose cannot tell
                // a moved row from a changed one, and there is nothing to animate between.
                items(items = rows, key = { it }) { row ->
                    AppCard(modifier = appAnimateItem()) {
                        AppText(
                            text = "Row $row",
                            style = AppTheme.typography.bodyLarge,
                            color = AppTheme.colors.contentPrimary,
                        )
                    }
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppButton(
                text = "Shuffle",
                onClick = { rows = rows.shuffled() },
                size = ButtonSize.Small,
            )
            AppButton(
                text = "Insert",
                onClick = { rows = listOf(rows.max() + 1) + rows },
                variant = ButtonVariant.Secondary,
                size = ButtonSize.Small,
            )
            AppButton(
                text = "Remove",
                onClick = { rows = rows.drop(1).ifEmpty { rows } },
                variant = ButtonVariant.Ghost,
                size = ButtonSize.Small,
            )
        }
    }
}

private val AccordionEntries = listOf(
    "What is a design system?" to
        "A set of components that already agree on colour, spacing and motion, so a screen is " +
        "assembled rather than styled.",
    "Why no Material?" to
        "Material is a brand as much as a toolkit. Owning the components means the theme is the " +
        "only thing that decides how anything looks.",
    "Can I change the shapes?" to
        "Every radius comes from AppTheme.shapes. Change it there and every component follows.",
)

private const val SWIPE_ROWS = 3
private const val PAGER_PAGES = 4
private const val PULL_ROWS = 8
private const val PAGE_SIZE = 5
private const val MAX_ROWS = 15
private const val ANIMATED_ROWS = 5
private const val REFRESH_MILLIS = 1_200L
private const val LOAD_MILLIS = 900L
