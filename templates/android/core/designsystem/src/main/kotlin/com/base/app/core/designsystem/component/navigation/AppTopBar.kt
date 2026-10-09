package com.base.app.core.designsystem.component.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.R
import com.base.app.core.designsystem.component.button.AppIconButton
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.LocalTopChrome
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.theme.AppTheme

/**
 * How far a list has scrolled from its top, in pixels, for a top bar that changes as it does. Past
 * the first item it is "a long way", which is all a bar needs to know.
 */
fun LazyListState.scrolledFromTop(): Int =
    if (firstVisibleItemIndex > 0) Int.MAX_VALUE else firstVisibleItemScrollOffset

/**
 * The inline top bar: a back button, a title, and actions. The title takes what the buttons leave,
 * so it never runs under a second action; [centerTitle] centres it in the bar when it fits.
 *
 * Content runs under the bar. With [scrolled], the bar is clear at the top of the content and its
 * surface and hairline fade in as content scrolls under it; without, it is always solid. Either way
 * it writes its height to the screen's top inset, which `chromeContentPadding()` reads.
 */
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    centerTitle: Boolean = true,
    scrolled: (() -> Int)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = AppTheme.colors
    val topChrome = LocalTopChrome.current
    val fadeDistance = ScrollFadeDistance
    Box(
        modifier = modifier
            .fillMaxWidth()
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                topChrome.value = placeable.height.toDp()
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            }
            .drawBehind {
                val shown = scrolled?.let { (it() / fadeDistance.toPx()).coerceIn(0f, 1f) } ?: 1f
                if (shown > 0f) {
                    drawRect(colors.surface, alpha = shown)
                    drawLine(colors.separator, Offset(0f, size.height - 0.5f), Offset(size.width, size.height - 0.5f), 1f, alpha = shown)
                }
            }
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        InlineBar(
            onBack = onBack,
            centerTitle = centerTitle,
            actions = actions,
            title = { TitleBlock(title, subtitle, centerTitle) },
        )
    }
}

/**
 * The large-title bar of a root screen: the title sits large under the bar and scrolls up into it,
 * handing over to a small centred title, as iOS's does. Collapsing changes only what is drawn: the
 * bar keeps its expanded size and writes it to the screen's top inset once, so scrolling recomposes
 * and re-measures nothing. Pass the list's position as [scrolled], `{ listState.scrolledFromTop() }`.
 */
@Composable
fun AppLargeTitleTopBar(
    title: String,
    scrolled: () -> Int,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = AppTheme.colors
    val topChrome = LocalTopChrome.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                topChrome.value = placeable.height.toDp()
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            }
            .drawBehind {
                // Once the large title has gone, content is under the small bar: show its surface.
                val area = LargeTitleArea.toPx().coerceAtMost(size.height)
                val statusAndInline = size.height - area
                if (scrolled() >= area) {
                    drawRect(colors.surface, size = Size(size.width, statusAndInline))
                    drawLine(colors.separator, Offset(0f, statusAndInline - 0.5f), Offset(size.width, statusAndInline - 0.5f), 1f)
                }
            }
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        InlineBar(
            onBack = onBack,
            centerTitle = true,
            actions = actions,
            title = {
                AppText(
                    text = title,
                    modifier = Modifier.graphicsLayer {
                        // Appears as the large title finishes leaving.
                        val area = LargeTitleArea.toPx()
                        alpha = ((scrolled() - area * 0.7f) / (area * 0.3f)).coerceIn(0f, 1f)
                    },
                    style = AppTheme.typography.headline,
                    color = colors.contentPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = LargeTitleArea)
                .graphicsLayer { clip = true },
            contentAlignment = Alignment.BottomStart,
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = AppTheme.spacing.gutter)
                    .padding(bottom = AppTheme.spacing.sm)
                    .graphicsLayer { translationY = -scrolled().coerceAtMost(size.height.toInt() + 1).toFloat() },
            ) {
                AppText(
                    text = title,
                    modifier = Modifier.semantics { heading() },
                    style = AppTheme.typography.largeTitle,
                    color = colors.contentPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                subtitle?.let {
                    AppText(text = it, style = AppTheme.typography.subheadline, color = colors.contentSecondary)
                }
            }
        }
    }
}

@Composable
private fun TitleBlock(title: String, subtitle: String?, centred: Boolean) {
    Column(horizontalAlignment = if (centred) Alignment.CenterHorizontally else Alignment.Start) {
        AppText(
            text = title,
            modifier = Modifier.semantics { heading() },
            style = AppTheme.typography.headline,
            color = AppTheme.colors.contentPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (centred) TextAlign.Center else TextAlign.Start,
        )
        subtitle?.let {
            AppText(
                text = it,
                style = AppTheme.typography.caption1,
                color = AppTheme.colors.contentSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Back button, title, actions in one pass. A centred title stays centred in the bar while it fits
 * between the two sides, and otherwise starts after the back button; either way it never overlaps.
 */
@Composable
private fun InlineBar(
    onBack: (() -> Unit)?,
    centerTitle: Boolean,
    actions: @Composable RowScope.() -> Unit,
    title: @Composable () -> Unit,
) {
    val edge = AppTheme.spacing.xs
    val titleStart = AppTheme.spacing.gutter - edge
    Layout(
        contents = listOf(
            {
                if (onBack != null) {
                    AppIconButton(
                        icon = AppIcons.ChevronLeft,
                        contentDescription = stringResource(R.string.designsystem_navigate_back),
                        onClick = onBack,
                        tint = AppTheme.colors.accentContent,
                    )
                }
            },
            title,
            { Row(horizontalArrangement = Arrangement.spacedBy(0.dp), verticalAlignment = Alignment.CenterVertically, content = actions) },
        ),
        modifier = Modifier.fillMaxWidth().heightIn(min = AppTheme.sizes.topBarHeight).padding(horizontal = edge),
    ) { (leadingSlot, titleSlot, actionSlot), constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val leading = leadingSlot.firstOrNull()?.measure(loose)
        val trailing = actionSlot.firstOrNull()?.measure(loose)
        val leadingWidth = leading?.width ?: 0
        val trailingWidth = trailing?.width ?: 0
        val width = constraints.maxWidth
        val gap = 4.dp.roundToPx()
        val side = maxOf(leadingWidth, trailingWidth)
        val centredRoom = width - side * 2 - gap * 2
        val titleConstraints = Constraints(maxWidth = (if (centerTitle && centredRoom > 0) centredRoom else width - leadingWidth - trailingWidth - gap * 2).coerceAtLeast(0))
        val text = titleSlot.first().measure(titleConstraints)
        val height = maxOf(constraints.minHeight, leading?.height ?: 0, trailing?.height ?: 0, text.height)
        layout(width, height) {
            leading?.placeRelative(0, (height - leading.height) / 2)
            trailing?.placeRelative(width - trailing.width, (height - trailing.height) / 2)
            val x = when {
                centerTitle && centredRoom > 0 -> (width - text.width) / 2
                leading == null -> titleStart.roundToPx()
                else -> leadingWidth + gap
            }
            text.placeRelative(x, (height - text.height) / 2)
        }
    }
}

/** How far content scrolls under a clear bar before the bar is fully solid. */
private val ScrollFadeDistance = 24.dp

/** The room the large title takes under the small bar. */
private val LargeTitleArea = 52.dp
