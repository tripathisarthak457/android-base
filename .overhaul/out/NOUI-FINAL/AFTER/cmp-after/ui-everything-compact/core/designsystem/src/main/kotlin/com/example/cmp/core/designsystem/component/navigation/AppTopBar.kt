package com.example.cmp.core.designsystem.component.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.cmp.core.designsystem.R
import com.example.cmp.core.designsystem.component.button.AppButtonDefaults
import com.example.cmp.core.designsystem.component.button.AppIconButton
import com.example.cmp.core.designsystem.component.container.LocalScaffoldBackground
import com.example.cmp.core.designsystem.component.sheet.LocalSheetSurface
import com.example.cmp.core.designsystem.component.sheet.chrome.AppSheetCloseButton
import com.example.cmp.core.designsystem.component.text.AppText
import com.example.cmp.core.designsystem.component.text.grownWithFontScale
import com.example.cmp.core.designsystem.foundation.draw.drawHairline
import com.example.cmp.core.designsystem.foundation.layout.ChromeInset
import com.example.cmp.core.designsystem.foundation.layout.LocalTopChrome
import com.example.cmp.core.designsystem.icon.AppIcons
import com.example.cmp.core.designsystem.preview.AppComponentPreviews
import com.example.cmp.core.designsystem.theme.AppTheme
import kotlinx.coroutines.flow.collectLatest

/**
 * How far a list has scrolled from its top, in pixels, for a top bar that changes as it does. Once
 * the first item has scrolled out of the list, padding included, it is "a long way", which is all a
 * bar needs to know.
 *
 * The first item is followed through the list's top padding, where it runs under the bar: the
 * first visible item index skips it as soon as it is behind the padding, which for a first row
 * shorter than the large title would snap the title shut part way through its collapse.
 */
public fun LazyListState.scrolledFromTop(): Int {
    val first = layoutInfo.visibleItemsInfo.firstOrNull() ?: return 0
    return if (first.index == 0) (-first.offset).coerceAtLeast(0) else Int.MAX_VALUE
}

/**
 * The inline top bar: a back button, a title, and actions. The title takes what the buttons leave,
 * so it never runs under a second action; [centerTitle] centres it in the bar when it fits.
 *
 * Content runs under the bar. With [scrolled], the bar is clear at the top of the content and its
 * surface and hairline fade in as content scrolls under it; without, it is always solid. Either way
 * it writes its height to the screen's top inset, which `chromeContentPadding()` reads. A bar that
 * is not at the top of the window (inside a pane or a demo) passes `WindowInsets(0)` as
 * [windowInsets], so it does not reserve the status bar's height; a sheet has already taken it.
 * Inside a sheet the bar takes the sheet's colour, and at the root of the sheet, where it would
 * have no Back, it shows the sheet's close button in its place.
 */
@Composable
public fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    centerTitle: Boolean = true,
    scrolled: (() -> Int)? = null,
    windowInsets: WindowInsets = WindowInsets.statusBars,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = AppTheme.colors
    val topChrome = LocalTopChrome.current
    val fadeDistance = AppTopBarDefaults.ScrollFadeDistance
    val fill = LocalSheetSurface.current?.container ?: colors.surface
    Box(
        modifier = modifier
            .fillMaxWidth()
            .writeHeightTo(topChrome)
            .drawBehind {
                val shown = scrolled?.let { (it() / fadeDistance.toPx()).coerceIn(0f, 1f) } ?: 1f
                if (shown > 0f) {
                    drawRect(fill, alpha = shown)
                    drawHairline(colors.separator, 0f, size.width, size.height - 1f, alpha = shown)
                }
            }
            .windowInsetsPadding(windowInsets),
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
 * as iOS's does. When it has gone, a small centred title fades in and the bar's surface appears
 * over the next few points of scroll. Pass the list's position as [scrolled],
 * `{ listState.scrolledFromTop() }`.
 *
 * An [accessory], usually a search field, sits under the large title and scrolls away with it. The
 * bar keeps its expanded size and writes it to the screen's top inset, so content starts below the
 * accessory, and collapsing changes only what is drawn: scrolling recomposes nothing and re-measures
 * nothing, apart from the one moment the small title is told to fade in or out.
 *
 * In a window too short for a large title (a phone held sideways), the bar is inline from the start.
 */
@Composable
public fun AppLargeTitleTopBar(
    title: String,
    scrolled: () -> Int,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    windowInsets: WindowInsets = WindowInsets.statusBars,
    accessory: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = AppTheme.colors
    val topChrome = LocalTopChrome.current
    val large = !AppTheme.containerClass.isShort
    // How far the content scrolls before the large title and the accessory have gone: their height.
    var collapseDistance by remember { mutableIntStateOf(0) }
    val inlineTitle = remember { Animatable(if (large) 0f else 1f) }
    val springs = AppTheme.springs
    val currentScrolled by rememberUpdatedState(scrolled)
    // The scroll position is read here, in the effect, and never in composition: a read there,
    // even through derivedStateOf, sends this bar's composition through a recompose pass in every
    // frame of a scroll.
    LaunchedEffect(large, springs) {
        snapshotFlow { !large || (collapseDistance > 0 && currentScrolled() >= collapseDistance) }
            .collectLatest { collapsed -> inlineTitle.animateTo(if (collapsed) 1f else 0f, springs.quickFade()) }
    }
    val surfaceFade = AppTopBarDefaults.SurfaceFadeDistance
    val screen = LocalScaffoldBackground.current.takeOrElse { colors.background }
    val fill = LocalSheetSurface.current?.container ?: colors.surface

    Column(
        modifier = modifier
            .fillMaxWidth()
            .writeHeightTo(topChrome)
            .drawBehind {
                val inline = size.height - collapseDistance
                // What is left of the large title's band stays the screen's colour, so content that
                // runs ahead of the title (past a short first row, or after a jump) goes under it
                // rather than across its text.
                val collapsedBy = scrolled().coerceIn(0, collapseDistance)
                if (large && collapsedBy < collapseDistance) {
                    drawRect(screen, topLeft = Offset(0f, inline), size = Size(size.width, (collapseDistance - collapsedBy).toFloat()))
                }
                // The surface comes in over the last few points of the collapse, under the small bar only.
                val fadeFrom = collapseDistance - surfaceFade.toPx()
                val shown = if (!large) 1f else ((scrolled() - fadeFrom) / surfaceFade.toPx()).coerceIn(0f, 1f)
                if (shown > 0f) {
                    drawRect(fill, size = Size(size.width, inline), alpha = shown)
                    drawHairline(colors.separator, 0f, size.width, inline - 1f, alpha = shown)
                }
            }
            .windowInsetsPadding(windowInsets),
    ) {
        InlineBar(
            onBack = onBack,
            centerTitle = true,
            actions = actions,
            title = {
                AppText(
                    text = title,
                    modifier = Modifier.graphicsLayer { alpha = inlineTitle.value },
                    style = AppTheme.typography.headline,
                    color = colors.contentPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { collapseDistance = it.height }
                .graphicsLayer { clip = true },
        ) {
            Column(
                modifier = Modifier.graphicsLayer {
                    translationY = -scrolled().coerceAtMost(collapseDistance + 1).toFloat()
                },
            ) {
                if (large) {
                    Column(
                        modifier = Modifier
                            .heightIn(min = AppTopBarDefaults.LargeTitleArea)
                            .padding(horizontal = AppTheme.spacing.gutter)
                            .padding(bottom = AppTheme.spacing.sm),
                        verticalArrangement = Arrangement.Bottom,
                    ) {
                        AppText(
                            text = title,
                            modifier = Modifier.semantics { heading() },
                            style = AppTheme.typography.largeTitle.grownWithFontScale(LargeTitleMaxScale),
                            color = colors.contentPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        subtitle?.let {
                            AppText(text = it, style = AppTheme.typography.subheadline, color = colors.contentSecondary)
                        }
                    }
                }
                accessory?.let {
                    // Without the large title above it, the accessory keeps the same gap from the
                    // bar's hairline as it has below the title.
                    val top = if (large) 0.dp else AppTheme.spacing.sm
                    Box(
                        Modifier
                            .padding(horizontal = AppTheme.spacing.gutter)
                            .padding(top = top, bottom = AppTheme.spacing.sm),
                    ) { it() }
                }
            }
        }
    }
}

/** Writes this bar's measured height to the screen's top inset, before the content is laid out. */
private fun Modifier.writeHeightTo(topChrome: ChromeInset): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    topChrome.value = placeable.height.toDp()
    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
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
                        colors = AppButtonDefaults.iconButtonColors(content = AppTheme.colors.accentContent),
                    )
                } else {
                    AppSheetCloseButton()
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
        val gap = AppTopBarDefaults.TitleGap.roundToPx()
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

/** The top bars' scroll-driven fades, the large title's room and the gap around the title. */
public object AppTopBarDefaults {
    public val TitleGap: Dp = 4.dp
    /** How far content scrolls under a clear bar before the bar is fully solid. */
    public val ScrollFadeDistance: Dp = 24.dp
    /** The room the large title takes under the small bar. */
    public val LargeTitleArea: Dp = 52.dp
    /** How much scroll the large-title bar's surface takes to fade in once the title has gone. */
    public val SurfaceFadeDistance: Dp = 10.dp
}

/** How far the large title grows with the font scale: as iOS's, 44 pt of 34 at the first accessibility size. */
private const val LargeTitleMaxScale = 1.3f

@AppComponentPreviews
@Composable
private fun AppTopBarPreview() {
    Column(
        modifier = Modifier.background(AppTheme.colors.background),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        AppTopBar(title = "Orders")
        AppTopBar(title = "Order #4021", subtitle = "Shipped", onBack = {}) {
            AppIconButton(icon = AppIcons.Share, contentDescription = "Share", onClick = {})
        }
        AppTopBar(title = "A conversation with a very long name that cannot fit", onBack = {}, centerTitle = false)
        AppLargeTitleTopBar(title = "Inbox", scrolled = { 0 })
    }
}
