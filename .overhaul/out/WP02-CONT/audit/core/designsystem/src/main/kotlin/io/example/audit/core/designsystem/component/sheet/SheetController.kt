package io.example.audit.core.designsystem.component.sheet

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.example.audit.core.designsystem.component.sheet.content.SheetSlots
import io.example.audit.core.designsystem.component.sheet.gesture.SheetNestedScroll
import io.example.audit.core.designsystem.component.sheet.gesture.exit
import io.example.audit.core.designsystem.component.sheet.layout.RulerImeSource
import io.example.audit.core.designsystem.component.sheet.layout.SheetFrameInfo
import io.example.audit.core.designsystem.component.sheet.layout.SheetImeSource
import io.example.audit.core.designsystem.component.sheet.layout.SheetLayers
import io.example.audit.core.designsystem.component.sheet.layout.SheetMeasureEnvironment
import io.example.audit.core.designsystem.component.sheet.stack.SheetStack
import io.example.audit.core.designsystem.component.sheet.stack.StackedSheet
import io.example.audit.core.designsystem.foundation.adaptive.SizeClass
import io.example.audit.core.designsystem.foundation.interaction.AppHaptic
import io.example.audit.core.designsystem.foundation.interaction.AppHaptics
import io.example.audit.core.designsystem.foundation.layout.ChromeInset
import io.example.audit.core.designsystem.foundation.layout.EntryVisibility
import io.example.audit.core.designsystem.foundation.layout.HostMotion
import io.example.audit.core.designsystem.theme.AppMotionStyle
import io.example.audit.core.designsystem.theme.AppSpring
import io.example.audit.core.designsystem.theme.AppSprings
import io.example.audit.core.designsystem.theme.SheetMaterial
import io.example.audit.core.designsystem.theme.springs
import io.example.audit.core.designsystem.theme.tokens.DesignTokens
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

/**
 * One sheet's behaviour around its state: the scope its content sees, the decisions about closing
 * its motion code consults, and what its content registered. Built once per sheet; the parameters of
 * each composition are written into it after that composition.
 */
@Stable
internal class SheetController(
    override val sheetState: AppSheetState,
    override val scope: CoroutineScope,
) : AppSheetScope, SheetHost {

    var modality: SheetModality by mutableStateOf(SheetModality.Modal)
    var dismissible by mutableStateOf(true)
    var properties by mutableStateOf(AppSheetProperties())
    var style: AppSheetStyle? by mutableStateOf(null)
    var paneTitle: String? by mutableStateOf(null)
    var onDismissRequest: () -> Unit = {}
    var onDetentChange: ((SheetDetent) -> Unit)? = null
    var onHidden: (() -> Unit)? = null

    /** The caller's `visible`, or whether its `item` is set; null for a sheet a presenter shows. */
    var callerShows: Boolean? = null

    override var springs: AppSprings by mutableStateOf(AppMotionStyle.Standard.springs())
    var asDialog = false
    var haptics: AppHaptics? = null
    override var density: Float = 1f
    var isLight = true
    var layoutDirection = LayoutDirection.Ltr

    /** How the owner entry is shown; read in placement, layer blocks and flows only. */
    var entry: EntryVisibility = EntryVisibility({ true }, { true }, { true })

    val slots = SheetSlots(this)
    val padding = SheetContentPadding()
    val nestedScroll = SheetNestedScroll(this)
    val stacked = StackedSheet(sheetState)
    var stack: SheetStack? = null

    /** Where the sheet reports moving its host's content; set while its motion effects are composed. */
    var hostMotion: HostMotion? = null

    /** The frame measure resolved; replaced only when a value in it changes. */
    var frame: SheetFrameInfo? by mutableStateOf(null)

    /** Where the sheet sits, resolved in composition from the placement class. */
    var placement by mutableStateOf(SheetPlacement.Bottom)
    var placementClass by mutableStateOf(SizeClass.Phone)

    /** A horizontal hinge in window pixels, while the window is in tabletop posture. */
    var fold: Rect? by mutableStateOf(null)

    /** The half of a window split by a vertical hinge the sheet lives in; null for the whole container. */
    var hingeHalf: Rect? by mutableStateOf(null)

    /** The pane a non-modal sheet is bounded by, in window pixels; null for the whole container. */
    var paneRect: () -> Rect? = { null }

    var measureEnvironment: SheetMeasureEnvironment? = null
    var imeSource: SheetImeSource = RulerImeSource
    var hideKeyboard: () -> Unit = {}
    var inspection = false

    /** The container's size, written by its measure before the sheet layer's, which reads it. */
    var windowSize = Size.Zero

    /** The bounds within the container, written by its measure when they change. */
    var boundsInWindow: Rect by mutableStateOf(Rect.Zero)

    /** The grabber lambda is set: whether it shows also depends on the stops. */
    var grabberWanted by mutableStateOf(true)

    /** The footer is pinned to the visible bottom; otherwise SheetColumn composes it at the content's end. */
    var footerPinned by mutableStateOf(true)

    /** The keyboard's height from the window's bottom, written by placement only when it changes. */
    var imeHeight by mutableFloatStateOf(0f)

    /** Where the keyboard's running animation ends, or [imeHeight] when it is not animating. */
    var imeTarget by mutableFloatStateOf(0f)

    /** Focus is in the sheet: only then does the keyboard lift it. */
    var focusInside by mutableStateOf(false)

    /** Touch exploration is on: Automatic content interaction scrolls, so every row is reachable. */
    var touchExploration by mutableStateOf(false)

    /** Whether what is behind is blocked: written by placement, read by the overlay host's semantics. */
    var blocking: Boolean
        get() = sheetState.blocking
        set(value) {
            sheetState.blocking = value
        }

    /**
     * How tall the content is with its own padding and without the scroll reach: measured whenever
     * the reach is off, infinite once it filled its room without it, NaN before the first measure.
     * Written by measure only when it changes, which measures once more.
     */
    var contentNatural by mutableFloatStateOf(Float.NaN)

    /** Before API 30, where the keyboard lift is springing toward. */
    var liftGoal by mutableFloatStateOf(0f)

    /** "Remove animations": the card stack keeps its dim and drops its scale. */
    var reduceMotion = false

    /** The predictive back gesture started at the window's left edge. */
    var backFromLeft = false

    /** What the current gesture is made with, recorded on its first event. */
    var gesturePointer = PointerType.Touch

    /** The surface, which takes focus while the sheet blocks what is behind it. */
    val surfaceFocus = FocusRequester()

    /** The call site's footer, which SheetColumn composes at the content's end while it cannot stay pinned. */
    var callSiteFooter: (@Composable AppSheetScope.() -> Unit)? by mutableStateOf(null)

    /** The bottom chrome a screen reused in the sheet pads for: the sheet's settled content padding. */
    val bottomChrome = ChromeInset()

    /** A Glass sheet's live backdrop draws its body, so the surface does not. */
    var liveBackdrop by mutableStateOf(false)

    private var keyboardDrag = 0f

    /**
     * Dragging the sheet itself 24 dp with the keyboard up puts the keyboard away, as a drag of
     * a scroll view does on iOS.
     */
    fun onSheetDragDelta(delta: Float) {
        if (imeTarget <= 0f || !focusInside) {
            keyboardDrag = 0f
            return
        }
        keyboardDrag += abs(delta)
        if (keyboardDrag >= DesignTokens.Sheet.Keyboard.dragHidesAfter.value * density) {
            keyboardDrag = 0f
            hideKeyboard()
        }
    }

    /** The dim that reaches this sheet while another is stacked on it. */
    fun coveredDim(): Color {
        if (!isCovered) return Color.Transparent
        val stack = stack ?: return Color.Transparent
        return stack.dim(stacked.level)
    }

    val layers = SheetLayers(this)

    val grabberShown: Boolean by derivedStateOf {
        val stops = sheetState.stops
        grabberWanted && !isCovered && (stops?.let { !it.single && it.count >= 2 } ?: (sheetState.detents.size >= 2))
    }

    /** The person can drag the sheet now: not while it leaves, is parked or covered, or while the caller holds it. */
    val dragEnabled: Boolean by derivedStateOf {
        properties.dragEnabled && sheetState.isVisible && !sheetState.isLeaving && !sheetState.parkedInstantly &&
            sheetState.parkProgress == 0f && !isCovered
    }

    /** The current leaving was the person's decision, so hide() asks nothing of the caller. */
    private var personDecided = false

    /** The material this sheet draws: the style's, with Automatic resolved by the design style. */
    var material by mutableStateOf(SheetMaterial.Solid)

    /** True while the sheet floats inset at a partial detent; read in draw before any per-frame value. */
    val floatingNow: Boolean by derivedStateOf {
        val frame = frame
        frame != null && frame.floats && sheetState.expansionFraction < 1f
    }

    override val presentSpring: AppSpring get() = if (asDialog) springs.dialog else springs.sheet

    override val crossFade: Boolean get() = properties.transition == SheetTransition.CrossFade

    override val dismissAllowed: Boolean get() = dismissible && slots.guard == null

    override val callerHides: Boolean get() = callerShows == false

    override val readyToPresent: Boolean get() = frame != null && !slots.pending && entry.isSettled

    /** The person can close it some way: a close button shows only then. */
    val closable: Boolean get() = dismissible || slots.guard != null

    override val contentPadding: PaddingValues get() = padding

    override val contentScrolled: Float
        get() {
            val ramp = frame?.scrollEdgeRamp ?: return 0f
            return if (ramp <= 0f) 0f else (nestedScroll.scrolled / ramp).coerceIn(0f, 1f)
        }

    override val isCovered: Boolean
        get() = stack?.coverOf(stacked) != null

    override fun requestDismiss() = attemptOrClose(SheetDismissSource.Request)

    override fun dismiss() = closeByPerson(SheetDismissSource.Request, velocity = 0f)

    /** The person tried to close the sheet by [source]: it closes, or the guard hears of it. */
    fun attemptOrClose(source: SheetDismissSource) {
        if (!sheetState.isVisible || sheetState.isLeaving) return
        if (dismissAllowed) closeByPerson(source, velocity = 0f) else attempt(source)
    }

    override fun haptic(haptic: AppHaptic) {
        haptics?.perform(haptic)
    }

    override fun onHiddenByCode() {
        if (callerShows == true && !personDecided) onDismissRequest()
    }

    override fun onDetentChanged(detent: SheetDetent) {
        onDetentChange?.invoke(detent)
    }

    override fun onGone() {
        personDecided = false
        onHidden?.invoke()
    }

    override fun leftComposition() {
        stack?.unregister(stacked)
        hostMotion?.report(sheetState, false)
    }

    override fun attempt(source: SheetDismissSource) {
        slots.guard?.onAttempt?.invoke(source)
    }

    override fun closeByPerson(source: SheetDismissSource, velocity: Float) {
        if (!sheetState.isVisible || sheetState.isLeaving) return
        personDecided = true
        sheetState.beginLeaving()
        onDismissRequest()
        // Undispatched, so the exit holds the mutex before a second press could start anything.
        scope.launch(start = CoroutineStart.UNDISPATCHED) { sheetState.exit(velocity) }
    }
}

/**
 * A sheet's content padding as the scroller reads it: snapshot state read in measure, so a new
 * value re-measures the scroller without recomposing it.
 */
@Stable
internal class SheetContentPadding : PaddingValues {
    var gutter: Dp by mutableStateOf(0.dp)
    var bottom: Dp by mutableStateOf(0.dp)

    /**
     * The content has applied the bottom padding at least once, so its height includes it. Only
     * then is the sheet's measure right to take the padding back out of that height.
     */
    var bottomRead = false
        private set

    override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp = gutter

    override fun calculateTopPadding(): Dp = 0.dp

    override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp = gutter

    override fun calculateBottomPadding(): Dp {
        bottomRead = true
        return bottom
    }
}
