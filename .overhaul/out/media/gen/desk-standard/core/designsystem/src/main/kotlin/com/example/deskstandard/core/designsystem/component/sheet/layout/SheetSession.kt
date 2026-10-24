package com.example.deskstandard.core.designsystem.component.sheet.layout

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.tappableElement
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.example.deskstandard.core.designsystem.component.sheet.AppSheetProperties
import com.example.deskstandard.core.designsystem.component.sheet.AppSheetScope
import com.example.deskstandard.core.designsystem.component.sheet.AppSheetState
import com.example.deskstandard.core.designsystem.component.sheet.AppSheetStyle
import com.example.deskstandard.core.designsystem.component.sheet.LocalSheetSurfaceProvider
import com.example.deskstandard.core.designsystem.component.sheet.SheetController
import com.example.deskstandard.core.designsystem.component.sheet.SheetDetent
import com.example.deskstandard.core.designsystem.component.sheet.SheetKeyboard
import com.example.deskstandard.core.designsystem.component.sheet.SheetModality
import com.example.deskstandard.core.designsystem.component.sheet.SheetPlacement
import com.example.deskstandard.core.designsystem.component.sheet.SheetSurface
import com.example.deskstandard.core.designsystem.component.sheet.back.SheetBack
import com.example.deskstandard.core.designsystem.component.sheet.back.SheetFocusEffect
import com.example.deskstandard.core.designsystem.component.sheet.content.LocalSheetContentWidth
import com.example.deskstandard.core.designsystem.component.sheet.content.LocalSheetSlots
import com.example.deskstandard.core.designsystem.component.sheet.content.sheetOwnerVisibility
import com.example.deskstandard.core.designsystem.component.sheet.stack.LocalSheetStack
import com.example.deskstandard.core.designsystem.foundation.adaptive.AppLayout
import com.example.deskstandard.core.designsystem.foundation.adaptive.LocalWindowMetrics
import com.example.deskstandard.core.designsystem.foundation.adaptive.SizeClass
import com.example.deskstandard.core.designsystem.foundation.adaptive.WindowWidth
import com.example.deskstandard.core.designsystem.foundation.interaction.rememberAppHaptics
import com.example.deskstandard.core.designsystem.foundation.layout.LocalBarFrame
import com.example.deskstandard.core.designsystem.foundation.layout.LocalPaneBounds
import com.example.deskstandard.core.designsystem.foundation.layout.LocalPresentationLevel
import com.example.deskstandard.core.designsystem.theme.AppTheme
import com.example.deskstandard.core.designsystem.theme.SheetMaterial
import com.example.deskstandard.core.designsystem.theme.tokens.DesignTokens

/**
 * One sheet with everything around it: its controller, written from each composition; its Back,
 * focus, parking and host-motion effects; its place in the window's stack; and its layout. Both
 * AppSheet overloads and AppSheetLayout draw through this.
 */
@Composable
internal fun SheetSession(
    state: AppSheetState,
    onDismissRequest: () -> Unit,
    modifier: Modifier,
    modality: SheetModality,
    dismissible: Boolean,
    properties: AppSheetProperties,
    style: AppSheetStyle,
    paneTitle: String?,
    onDetentChange: ((SheetDetent) -> Unit)?,
    onHidden: (() -> Unit)?,
    callerShows: Boolean?,
    grabber: (@Composable AppSheetScope.() -> Unit)?,
    header: (@Composable AppSheetScope.() -> Unit)?,
    footer: (@Composable AppSheetScope.() -> Unit)?,
    content: @Composable AppSheetScope.() -> Unit,
    asDialog: Boolean = false,
    destination: Boolean = false,
) {
    val upThrough = (modality as? SheetModality.NonModal)?.upThrough
    require(upThrough == null || upThrough in state.detents) { "upThrough $upThrough is not one of ${state.detents}" }
    val scope = rememberCoroutineScope()
    val controller = remember(state) { SheetController(state, scope) }
    val stack = LocalSheetStack.current
    val entry = sheetOwnerVisibility()
    val ownerLevel = LocalPresentationLevel.current
    val paneBounds = LocalPaneBounds.current
    val barFrame = LocalBarFrame.current
    val springs = AppTheme.springs
    val haptics = rememberAppHaptics()
    val colors = AppTheme.colors
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val reduceMotion = AppTheme.reduceMotion
    val inspection = LocalInspectionMode.current
    val imeSource = LocalSheetImeSource.current
    val keyboard = LocalSoftwareKeyboardController.current
    val designMaterial = AppTheme.style.sheetMaterial
    val safeArea = WindowInsets.systemBars.union(WindowInsets.displayCutout)
    val navigationBars = WindowInsets.navigationBars
    val tappable = WindowInsets.tappableElement
    val environment = remember(safeArea, navigationBars, tappable, barFrame) {
        SheetMeasureEnvironment(safeArea, navigationBars, tappable, barFrame)
    }

    val modal = modality is SheetModality.Modal
    val windowClass = AppTheme.windowClass
    val posture = AppTheme.posture
    val metrics = LocalWindowMetrics.current
    val hinge = posture.hinge
    val fold = hinge?.takeIf { it.width >= it.height }
    val half by remember(hinge, direction, paneBounds, metrics, density) {
        derivedStateOf {
            val window = metrics.sizeDp
            with(density) { hingeHalf(window.width.toPx(), window.height.toPx(), hinge, paneBounds.rect?.center?.x, direction) }
        }
    }
    val paneClass by remember(paneBounds, density) {
        derivedStateOf {
            paneBounds.rect?.let { with(density) { SizeClass.of(it.width.toDp().value, it.height.toDp().value) } }
        }
    }
    val placementClass = half?.let { with(density) { SizeClass.of(it.width.toDp().value, it.height.toDp().value) } }
        ?: if (modal) windowClass else paneClass ?: windowClass
    val placement = resolvePlacement(properties.placement, modal, placementClass, tabletop = fold != null)
    val layout = AppLayout.forClass(placementClass)
    // Only the class of this width matters to the content: it is what its adaptive layouts read.
    val contentWidth = when {
        placement != SheetPlacement.Bottom && placement != SheetPlacement.Center -> layout.supportingPaneWidth
        placement == SheetPlacement.Center -> layout.formSheetWidth
        placementClass.width == WindowWidth.Compact -> layout.listPaneWidth
        else -> layout.sheetMaxWidth
    }
    val resolvedStyle = controller.slots.style?.style ?: style
    val material = if (resolvedStyle.material == SheetMaterial.Automatic) designMaterial else resolvedStyle.material
    val surface = remember(controller, material, resolvedStyle.colors.container) {
        SheetSurface(material, resolvedStyle.colors.container, controller)
    }

    SideEffect {
        controller.modality = modality
        controller.dismissible = dismissible
        controller.properties = properties
        controller.style = resolvedStyle
        controller.paneTitle = paneTitle
        controller.onDismissRequest = onDismissRequest
        controller.onDetentChange = onDetentChange
        controller.onHidden = onHidden
        controller.callerShows = callerShows
        controller.springs = springs
        controller.asDialog = asDialog
        controller.haptics = haptics
        controller.density = density.density
        controller.isLight = colors.isLight
        controller.layoutDirection = direction
        controller.reduceMotion = reduceMotion
        controller.inspection = inspection
        controller.entry = entry
        controller.measureEnvironment = environment
        controller.imeSource = imeSource
        controller.hideKeyboard = { keyboard?.hide() }
        controller.placement = placement
        controller.placementClass = placementClass
        controller.fold = fold
        controller.hingeHalf = half
        controller.paneRect = { paneBounds.rect }
        controller.grabberWanted = grabber != null
        controller.callSiteFooter = footer
        controller.material = material
        controller.stacked.modal = modal
        controller.stacked.destination = destination
        controller.stacked.scrim = resolvedStyle.colors.scrim
        controller.stacked.container = resolvedStyle.colors.container
        controller.stacked.overlay =
            if (colors.isLight) DesignTokens.Sheet.CardStack.overlayColorLight else DesignTokens.Sheet.CardStack.overlayColorDark
        state.ownerTabSelected = { entry.isTabSelected }
        if (inspection && callerShows == true && !state.isVisible) state.restoredDetent = state.initialDetent
    }
    DisposableEffect(state, controller) {
        state.host = controller
        onDispose { if (state.host === controller) state.host = null }
    }
    SheetStackEntry(controller, stack, ownerLevel)
    SheetMotionEffects(controller, expandForKeyboard = properties.keyboard == SheetKeyboard.Expand)
    SheetBack(controller)
    SheetFocusEffect(controller)
    SheetPrepareEffect(controller)
    CompositionLocalProvider(
        LocalSheetSlots provides controller.slots,
        LocalSheetContentWidth provides contentWidth,
        LocalSheetSurfaceProvider provides surface,
    ) {
        SheetLayout(controller, modifier, modal, placement, paneTitle, grabber, header, footer, content)
    }
}
