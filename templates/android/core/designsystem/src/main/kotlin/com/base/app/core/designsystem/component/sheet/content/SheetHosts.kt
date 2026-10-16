package com.base.app.core.designsystem.component.sheet.content

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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.base.app.core.designsystem.component.sheet.AppSheetScope
import com.base.app.core.designsystem.component.sheet.LocalSheetSurfaceProvider
import com.base.app.core.designsystem.component.sheet.SheetController
import com.base.app.core.designsystem.component.sheet.layout.sheetBringIntoView
import com.base.app.core.designsystem.foundation.ChromeInset
import com.base.app.core.designsystem.foundation.LocalBottomChrome
import com.base.app.core.designsystem.foundation.LocalTabVisible
import com.base.app.core.designsystem.foundation.LocalTopChrome
import com.base.app.core.designsystem.foundation.adaptive.ProvideContainerWidth
import com.base.app.core.designsystem.foundation.layout.EntryVisibility
import com.base.app.core.designsystem.foundation.layout.LocalEntryVisibility
import com.base.app.core.designsystem.foundation.layout.LocalPresentationLevel
import com.base.app.core.designsystem.theme.tokens.DesignTokens

/**
 * The header slot: what the content provided, or the call site's header. It is pinned above the
 * content in its own layer and draws the scroll-edge fade over the content's top while the content
 * is scrolled under it (M27), read in draw.
 */
@Composable
internal fun SheetHeaderHost(controller: SheetController, header: (@Composable AppSheetScope.() -> Unit)?) {
    val provided = controller.slots.header
    if (provided == null && header == null) {
        SideEffect { controller.slots.composed() }
        return
    }
    Box(Modifier.consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)).scrollEdgeFade(controller)) {
        SheetLocals(controller) {
            if (provided != null) provided.Compose(controller) else header?.invoke(controller)
        }
    }
    SideEffect { controller.slots.composed() }
}

/**
 * The footer slot. It consumes the navigation bar and the keyboard, because the sheet places it
 * above both: an `AppBottomActionBar` in it pads nothing twice. While it cannot stay pinned it is
 * composed at the end of the content instead, by [SheetColumn] or [sheetFooterItem].
 */
@Composable
internal fun SheetFooterHost(controller: SheetController, footer: (@Composable AppSheetScope.() -> Unit)?) {
    if (!controller.footerPinned) {
        SideEffect { controller.slots.composed() }
        return
    }
    SheetFooterContent(controller, footer)
    SideEffect { controller.slots.composed() }
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

/** The locals every slot of a sheet sees. */
@Composable
private fun SheetLocals(controller: SheetController, content: @Composable () -> Unit) {
    val owner = LocalEntryVisibility.current
    val covered = controller.isCovered
    val visibility = remember(controller, owner) {
        EntryVisibility(
            tabSelected = { owner.isTabSelected },
            uncovered = { owner.isUncovered && !controller.isCovered && controller.sheetState.parkProgress == 0f },
            settled = { owner.isSettled },
        )
    }
    ProvideContainerWidth(controller.contentWidth) {
        CompositionLocalProvider(
            LocalSheetSurfaceProvider provides controller.surface,
            LocalSheetSlots provides controller.slots,
            LocalPresentationLevel provides controller.stacked.level,
            LocalEntryVisibility provides visibility,
            LocalTabVisible provides (LocalTabVisible.current && !covered),
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
