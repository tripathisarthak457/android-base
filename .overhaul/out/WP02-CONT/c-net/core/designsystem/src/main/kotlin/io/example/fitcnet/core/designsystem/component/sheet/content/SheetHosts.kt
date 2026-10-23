package io.example.fitcnet.core.designsystem.component.sheet.content

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import io.example.fitcnet.core.designsystem.component.sheet.AppSheetScope
import io.example.fitcnet.core.designsystem.component.sheet.SheetController
import io.example.fitcnet.core.designsystem.component.sheet.layout.sheetBringIntoView
import io.example.fitcnet.core.designsystem.foundation.adaptive.ProvideContainerWidth
import io.example.fitcnet.core.designsystem.foundation.layout.ChromeInset
import io.example.fitcnet.core.designsystem.foundation.layout.EntryVisibility
import io.example.fitcnet.core.designsystem.foundation.layout.LocalBottomChrome
import io.example.fitcnet.core.designsystem.foundation.layout.LocalEntryVisibility
import io.example.fitcnet.core.designsystem.foundation.layout.LocalPresentationLevel
import io.example.fitcnet.core.designsystem.foundation.layout.LocalTabVisible
import io.example.fitcnet.core.designsystem.foundation.layout.LocalTopChrome
import io.example.fitcnet.core.designsystem.foundation.layout.TabVisibility
import io.example.fitcnet.core.designsystem.theme.tokens.DesignTokens

/**
 * The header slot: what the content provided, or the call site's header. It is pinned above the
 * content in its own layer and draws the scroll-edge fade over the content's top while the content
 * is scrolled under it, read in draw.
 */
@Composable
internal fun SheetHeaderHost(controller: SheetController, header: (@Composable AppSheetScope.() -> Unit)?) {
    val provided = controller.slots.header
    if (provided == null && header == null) {
        SideEffect { controller.slots.markComposed() }
        return
    }
    Box(Modifier.consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)).scrollEdgeFade(controller)) {
        SheetLocals(controller) {
            if (provided != null) provided.Compose(controller) else header?.invoke(controller)
        }
    }
    SideEffect { controller.slots.markComposed() }
}

/**
 * The footer slot. It consumes the navigation bar and the keyboard, because the sheet places it
 * above both: an `AppBottomActionBar` in it pads nothing twice. While it cannot stay pinned it is
 * composed at the end of the content instead, by [SheetColumn] or [sheetFooterItem].
 */
@Composable
internal fun SheetFooterHost(controller: SheetController, footer: (@Composable AppSheetScope.() -> Unit)?) {
    if (!controller.footerPinned) {
        SideEffect { controller.slots.markComposed() }
        return
    }
    SheetFooterContent(controller, footer)
    SideEffect { controller.slots.markComposed() }
}

@Composable
internal fun SheetFooterContent(controller: SheetController, footer: (@Composable AppSheetScope.() -> Unit)?) {
    val provided = controller.slots.footer
    if (provided == null && footer == null) return
    Box(Modifier.consumeWindowInsets(WindowInsets.navigationBars.union(WindowInsets.ime))) {
        SheetLocals(controller) {
            if (provided != null) provided.Compose(controller) else footer?.invoke(controller)
        }
    }
}

/**
 * The content box: one root at the top start, measured at the sheet's width with Large's height as
 * its most. It hands its scroll to the sheet, consumes every inset the sheet already keeps
 * clear, and gives the content the locals of the sheet rather than those of the screen that opened
 * it.
 */
@Composable
internal fun SheetContentHost(controller: SheetController, content: @Composable AppSheetScope.() -> Unit) {
    Box(
        Modifier
            .nestedScroll(controller.nestedScroll)
            .consumeWindowInsets(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Top).union(WindowInsets.navigationBars).union(WindowInsets.ime),
            )
            .sheetBringIntoView(controller),
    ) {
        SheetLocals(controller) { content(controller) }
    }
}

/**
 * How a sheet presented from another sheet's content sees its owner. The content counts as covered
 * while a sheet is stacked on its sheet, but a sheet presented from it is that cover: judged by the
 * content's visibility it would park itself, uncover its owner and come back, every frame.
 */
internal class SheetPresenting(val content: EntryVisibility, val owner: EntryVisibility)

private val LocalSheetPresenting = staticCompositionLocalOf<SheetPresenting?> { null }

/**
 * The width of the sheet's size class, provided by the session's composition with the sheet's
 * surface: the content's adaptive layouts and bars read both in their first composition, before the
 * controller could hold them.
 */
internal val LocalSheetContentWidth = staticCompositionLocalOf { 0.dp }

/** The visibility of whatever presents a sheet composed here: a screen's entry, or the sheet it was opened from. */
@Composable
@ReadOnlyComposable
internal fun sheetOwnerVisibility(): EntryVisibility {
    val local = LocalEntryVisibility.current
    val presenting = LocalSheetPresenting.current
    return if (presenting != null && local === presenting.content) presenting.owner else local
}

/** The locals every slot of a sheet sees. */
@Composable
private fun SheetLocals(controller: SheetController, content: @Composable () -> Unit) {
    val owner = sheetOwnerVisibility()
    val tab = LocalTabVisible.current
    val visibility = remember(tab, controller) { TabVisibility { tab.isVisible && !controller.isCovered } }
    val presenting = remember(controller, owner) {
        SheetPresenting(
            content = EntryVisibility(
                tabSelected = { owner.isTabSelected },
                uncovered = { owner.isUncovered && !controller.isCovered && controller.sheetState.parkProgress == 0f },
                settled = { owner.isSettled },
            ),
            owner = EntryVisibility(
                tabSelected = { owner.isTabSelected },
                uncovered = { owner.isUncovered && controller.sheetState.parkProgress == 0f },
                settled = { owner.isSettled },
            ),
        )
    }
    ProvideContainerWidth(LocalSheetContentWidth.current) {
        CompositionLocalProvider(
            LocalSheetSlots provides controller.slots,
            LocalPresentationLevel provides controller.stacked.level,
            LocalEntryVisibility provides presenting.content,
            LocalSheetPresenting provides presenting,
            LocalTabVisible provides visibility,
            LocalTopChrome provides remember { ChromeInset() },
            LocalBottomChrome provides controller.bottomChrome,
            content = content,
        )
    }
}

private fun Modifier.scrollEdgeFade(controller: SheetController): Modifier = drawWithCache {
    val color = controller.style?.colors?.container ?: Color.Transparent
    val height = DesignTokens.Sheet.Header.scrollEdgeHeight.toPx()
    val fade = Brush.verticalGradient(listOf(color, Color.Transparent), startY = size.height, endY = size.height + height)
    val fadeSize = Size(size.width, height)
    val topLeft = Offset(0f, size.height)
    onDrawWithContent {
        drawContent()
        val scrolled = controller.contentScrolled
        if (scrolled > 0f) drawRect(brush = fade, topLeft = topLeft, size = fadeSize, alpha = scrolled)
    }
}
