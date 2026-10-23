package io.example.fitcnetdb.core.designsystem.component.sheet.back

import androidx.compose.animation.core.animate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventHandler
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import io.example.fitcnetdb.core.designsystem.component.overlay.LocalOverlayHost
import io.example.fitcnetdb.core.designsystem.component.sheet.SheetController
import io.example.fitcnetdb.core.designsystem.component.sheet.SheetDismissSource
import io.example.fitcnetdb.core.designsystem.component.sheet.SheetModality
import io.example.fitcnetdb.core.designsystem.component.sheet.gesture.settleTo
import io.example.fitcnetdb.core.designsystem.component.sheet.gesture.stopMotion
import io.example.fitcnetdb.core.designsystem.theme.tokens.DesignTokens
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

/**
 * The sheet's Back: one handler at overlay priority on the dispatcher of the
 * composition that shows the sheet, so it is asked before any screen's and is off with a kept tab
 * that is not on screen, and while the window's overlays are suspended. Its enablement comes from a
 * flow over state that changes a few times per opening, never from composition, so nothing
 * recomposes for it.
 *
 * The gesture follows Material's bottom container: the sheet shrinks toward its bottom edge as the
 * finger moves and leaves in one gesture on commit. A guarded sheet resists and asks its guard; a
 * persistent one steps down a detent; with the keyboard up, Back closes the keyboard.
 */
@Composable
internal fun SheetBack(controller: SheetController) {
    val dispatcher = LocalNavigationEventDispatcherOwner.current?.navigationEventDispatcher ?: return
    val host = LocalOverlayHost.current
    val handler = remember(controller) { SheetBackHandler(controller) }
    DisposableEffect(dispatcher, handler) {
        dispatcher.addHandler(handler, NavigationEventDispatcher.PRIORITY_OVERLAY)
        onDispose { handler.remove() }
    }
    LaunchedEffect(handler, host) {
        snapshotFlow { host?.suspended != true && controller.backEnabled() }
            .collect { handler.isBackEnabled = it }
    }
}

/** The front sheet takes Back while it has a way out or a step to take. */
private fun SheetController.backEnabled(): Boolean {
    val state = sheetState
    if (!state.isVisible || state.parkedInstantly || state.parkProgress > 0f) return false
    if (stack?.isFront(stacked) == false) return false
    if (stacked.destination && state.isLeaving) return false
    val modality = modality
    if (modality is SheetModality.NonModal && !dismissible) {
        val stops = state.stops ?: return false
        val lowest = stops.detents[stops.count - 1]
        return state.currentDetent != lowest || imeTarget > 0f
    }
    return true
}

private enum class BackBranch { Keyboard, Content, StepDown, Guarded, Dismiss, Consume }

/** What Back does now, in order. */
private fun SheetController.backBranch(): BackBranch {
    val state = sheetState
    return when {
        state.isLeaving -> BackBranch.Consume
        imeTarget > 0f && focusInside -> BackBranch.Keyboard
        slots.stepBack != null -> BackBranch.Content
        modality is SheetModality.NonModal && !dismissible -> BackBranch.StepDown
        !dismissAllowed -> BackBranch.Guarded
        else -> BackBranch.Dismiss
    }
}

/**
 * Back from a key, Escape or a gesture's commit, with no preview. [source] is what a guard is
 * told; Escape reports itself.
 */
internal fun SheetController.backWithoutPreview(source: SheetDismissSource) {
    when (backBranch()) {
        BackBranch.Keyboard -> hideKeyboard()
        BackBranch.Content -> slots.stepBack?.onBack?.invoke()
        BackBranch.StepDown -> scope.launch { sheetState.collapse() }
        BackBranch.Guarded -> attempt(source)
        BackBranch.Dismiss -> closeByPerson(source, velocity = 0f)
        BackBranch.Consume -> Unit
    }
}

private class SheetBackHandler(private val controller: SheetController) :
    NavigationEventHandler<NavigationEventInfo>(NavigationEventInfo.None, isBackEnabled = false) {

    private var branch = BackBranch.Dismiss
    private var gesture = false
    private var interrupted = false

    override fun onBackStarted(event: NavigationEvent) {
        val state = controller.sheetState
        branch = controller.backBranch()
        gesture = true
        controller.backFromLeft = event.swipeEdge == NavigationEvent.EDGE_LEFT
        // A gesture during a present or a settle holds the sheet where it is; the preview runs
        // from there, and a cancel lets it finish where it was going.
        interrupted = state.animating && !state.isLeaving && !state.dragging
        if (interrupted) controller.scope.launch(start = CoroutineStart.UNDISPATCHED) { state.stopMotion() }
        progress(event.progress)
    }

    override fun onBackProgressed(event: NavigationEvent) = progress(event.progress)

    override fun onBackCancelled() {
        gesture = false
        val state = controller.sheetState
        val springs = controller.springs
        controller.scope.launch {
            animate(state.backProgress, 0f, animationSpec = springs.sheet.unit) { value, _ -> state.backProgress = value }
        }
        controller.scope.launch {
            animate(state.backFollow, 0f, animationSpec = springs.sheet.pixels) { value, _ -> state.backFollow = value }
        }
        if (interrupted) {
            interrupted = false
            val target = state.targetDetent ?: state.currentDetent
            if (target != null) controller.scope.launch { state.settleTo(target, velocity = 0f, byPerson = false) }
        }
    }

    override fun onBackCompleted() {
        val state = controller.sheetState
        val previewed = gesture
        gesture = false
        interrupted = false
        if (!previewed) branch = controller.backBranch()
        when (branch) {
            BackBranch.StepDown -> controller.scope.launch {
                state.collapse()
                animate(state.backFollow, 0f, animationSpec = controller.springs.sheet.pixels) { value, _ -> state.backFollow = value }
            }
            BackBranch.Guarded -> {
                controller.scope.launch {
                    animate(state.backProgress, 0f, animationSpec = controller.springs.sheet.unit) { value, _ -> state.backProgress = value }
                }
                controller.attempt(SheetDismissSource.Back)
            }
            else -> controller.backWithoutPreview(SheetDismissSource.Back)
        }
    }

    private fun progress(raw: Float) {
        val state = controller.sheetState
        val eased = DesignTokens.Sheet.Back.easing.transform(raw.coerceIn(0f, 1f))
        when (branch) {
            BackBranch.Dismiss -> state.backProgress = eased
            BackBranch.Guarded -> state.backProgress = eased * DesignTokens.Sheet.Back.guardedShare
            BackBranch.StepDown -> state.backFollow = DesignTokens.Sheet.Back.persistentFollow.value * controller.density * eased
            else -> Unit
        }
    }
}
