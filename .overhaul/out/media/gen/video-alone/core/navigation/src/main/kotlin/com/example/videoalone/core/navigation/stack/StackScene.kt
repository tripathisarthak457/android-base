package com.example.videoalone.core.navigation.stack

import androidx.compose.animation.core.animate
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.MutableRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.navigation3.runtime.NavEntry
import androidx.navigationevent.NavigationEvent
import com.example.videoalone.core.common.navigation.AppNavKey
import com.example.videoalone.core.navigation.back.BackCardHost
import com.example.videoalone.core.navigation.back.BackCardState
import com.example.videoalone.core.navigation.back.BackGate
import com.example.videoalone.core.navigation.back.commitPop
import com.example.videoalone.core.navigation.shell.CardCutout
import com.example.videoalone.core.navigation.transition.ScreenMotion
import kotlin.math.ceil
import kotlin.math.floor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Everything a stack host shows, in one parent that never changes, as iOS keeps a navigation
 * controller's views: the page on top, the page beneath it and two more under that stay composed,
 * measured and placed, and are parked (drawn at alpha 0, off the window, out of the semantics tree)
 * while covered. Showing one again changes two layer properties: nothing is composed, measured or
 * recorded on the first frame of a pop or a back gesture.
 *
 * Moving an entry between parents would detach it, dropping every layer it had, and unplacing it
 * would do the same; both are what made a pop's first frame cost as much as composing the screen.
 *
 * The slide is one value, [cover] (how far the upper page covers the lower one), animated after the
 * frame that composes a new screen, so that heavy frame shows nothing moving. Every pose is read in
 * layer and draw blocks.
 */
@Stable
internal class StackScene(private val gate: BackGate) : BackCardHost {

    override val card = BackCardState()

    var upper: Map<Any, Slot> by mutableStateOf(emptyMap())
        private set

    var lower: Map<Any, Slot> by mutableStateOf(emptyMap())
        private set

    /** The shown entries of the page on top, after the last change. */
    var top: Set<Any> by mutableStateOf(emptySet())
        private set

    var beneath: Set<Any> by mutableStateOf(emptySet())
        private set

    /** Content keys of the entries that hide the tab bar. */
    var hidingBar: Set<Any> by mutableStateOf(emptySet())
        private set

    // Whether an entry of [upper] or of [lower] hides the bar: worked out when they change, since
    // [barCover] is read several times on every frame of a slide.
    private var upperHidesBar by mutableStateOf(false)
    private var lowerHidesBar by mutableStateOf(false)

    /** 0: the upper page off the window, 1: in place over the lower one. */
    var cover by mutableFloatStateOf(1f)
        private set

    var moving by mutableStateOf(false)
        private set

    /** The host's size, direction and density, written by its layout. */
    var width by mutableFloatStateOf(0f)
    var height by mutableFloatStateOf(0f)
    var rtl by mutableStateOf(false)
    var density: Density = Density(1f)

    /** Where each laid-out entry's left edge is in the host, written by its layout. */
    val lefts = HashMap<Any, Float>()

    /** Each laid-out entry's width, written by its layout: a pane is narrower than the host. */
    val widths = HashMap<Any, Float>()

    var motion = ScreenMotion()

    /** The plan on screen: what composition diffs the next change against. */
    var applied: StackPlan? = null
        private set

    /**
     * The last plan whose slide has ended. Read in composition, so the frame that ends a slide is
     * the one that lets go of the entries it carried off the stack.
     */
    var settled: StackPlan? by mutableStateOf(null)
        private set

    /** The entries of [applied], with those it slides off the stack, so they can be drawn while they leave. */
    private var appliedEntries: Map<Any, NavEntry<AppNavKey>> = emptyMap()

    private var velocity = 0f
    private var job: Job? = null
    private val shadowBrushes = HashMap<Int, Brush>()
    private val cardHole = MutableRect(0f, 0f, 0f, 0f)

    /**
     * The entries [plan] slides that are no longer on the stack: a popped page sliding out, or the
     * page a replace slides over. Empty once the slide has ended.
     */
    fun departing(plan: StackPlan, keys: Set<Any>): List<NavEntry<AppNavKey>> {
        if (plan.slide == Slide.None || settled === plan) return emptyList()
        return (plan.upper.keys + plan.lower.keys).mapNotNull { key -> if (key in keys) null else appliedEntries[key] }
    }

    fun appliedEntry(key: Any): NavEntry<AppNavKey>? = appliedEntries[key]

    /** Makes [plan] the one on screen. Called after the composition that drew it, once per plan. */
    fun apply(plan: StackPlan, entries: List<NavEntry<AppNavKey>>, hiding: Set<Any>, scope: CoroutineScope) {
        if (plan === applied) return
        job?.cancel()
        val current = entries.associateBy { it.contentKey }
        appliedEntries = current + departing(plan, current.keys).associateBy { it.contentKey }
        applied = plan
        hidingBar = hiding
        top = plan.top.shown.keys
        beneath = plan.beneath?.shown?.keys.orEmpty()
        upper = plan.upper
        lower = plan.lower
        updateBarHiding()
        velocity = 0f
        when (plan.slide) {
            Slide.None -> {
                cover = 1f
                moving = false
                gate.noMotion()
            }
            Slide.Push -> {
                cover = 0f
                job = scope.launch { slideTo(1f) }
            }
            Slide.Pop -> {
                cover = 1f
                job = scope.launch { slideTo(0f) }
            }
        }
    }

    // Launched after the composition that changed the stack, so the first frame of the slide is the
    // one after the frame that composed the arriving screen.
    private suspend fun slideTo(target: Float) {
        moving = true
        gate.report(this, true)
        try {
            animate(cover, target, velocity, motion.fraction) { value, speed ->
                cover = value
                velocity = speed
                val travelled = if (target == 1f) value else 1f - value
                if (travelled >= BackGate.SETTLED_ENOUGH) gate.report(this, false)
            }
            val plan = applied ?: return
            upper = plan.top.shown
            lower = plan.beneath?.shown.orEmpty()
            updateBarHiding()
            cover = 1f
            moving = false
            settled = plan
        } finally {
            gate.report(this, false)
        }
    }

    private fun updateBarHiding() {
        upperHidesBar = upper.keys.any { it in hidingBar }
        lowerHidesBar = lower.keys.any { it in hidingBar }
    }

    /** How far the screen that hides the tab bar covers the last one that shows it, for [BarAnchor]. */
    fun barCover(): Float {
        val upperHides = upperHidesBar
        val lowerHides = lowerHidesBar
        val covered = when {
            upperHides && lowerHides -> 1f
            upperHides -> cover
            lowerHides -> 1f - cover
            else -> 0f
        }
        val revealed = if (card.active && upperHides && !lowerHides) card.reveal else 0f
        return covered * (1f - revealed)
    }

    /**
     * While the back card lifts a screen that hides the tab bar off one that shows it, writes the
     * card's outline into [into], in the host's px (the window's, on the widths with a bar).
     */
    fun barCutout(into: CardCutout): Boolean {
        val subject = card.subject
        if (!card.active || subject == null || subject !in hidingBar || card.beneath.any { it in hidingBar }) return false
        val w = width
        val h = height
        val scale = card.scale
        val slide = if (subject in upper) (if (rtl) -1f else 1f) * w * (1f - cover) else 0f
        val centerX = (lefts[subject] ?: 0f) + w / 2f + slide + with(card) { density.shiftX(w) }
        val centerY = h / 2f + with(card) { density.shiftY(h) }
        into.left = centerX - scale * w / 2f
        into.right = centerX + scale * w / 2f
        into.top = centerY - scale * h / 2f
        into.bottom = centerY + scale * h / 2f
        into.corner = with(card) { density.corner() } * scale
        into.scrim = card.scrim.value
        into.opacity = card.contentAlpha.value
        return true
    }

    /** The lifecycle an entry's content may reach: the top resumed, the page under it started, the rest created. */
    fun capOf(key: Any): Lifecycle.State = when {
        key in top -> Lifecycle.State.RESUMED
        // Raised only once the screens stop, so a pop never restarts the collectors of a screen
        // that is still out of sight in the frame that starts it.
        key in beneath && (!moving || key in lower) -> Lifecycle.State.STARTED
        else -> Lifecycle.State.CREATED
    }

    /** Whether the back card may start: the top page is one entry, and nothing but a push is moving. */
    override fun canStartCard(): Boolean =
        !card.active && top.size == 1 && beneath.isNotEmpty() && (!moving || applied?.slide == Slide.Push)

    override fun startCard(event: NavigationEvent, scope: CoroutineScope) {
        card.start(event, subject = top.single(), beneath = beneath, midPush = moving, scope = scope)
    }

    override suspend fun cancelCard() {
        // A pane narrower than the host leaves the page beside it under the scrim, still in view.
        val fullWidth = (card.subject?.let { widths[it] } ?: width) >= width
        card.cancel(midPush = moving, fullWidth = fullWidth)
    }

    override suspend fun commitCard(pop: (Int) -> Int, releasedNanos: Long, scope: CoroutineScope) {
        card.commitPop(gate, motion.fraction, pop, releasedNanos, scope)
    }

    fun GraphicsLayerScope.pose(key: Any) {
        val w = width
        val toward = if (rtl) -1f else 1f
        when {
            key in upper -> {
                translationX = toward * w * (1f - cover)
                if (card.active && key == card.subject) with(card) { applyCard() }
            }
            card.active && key in card.beneath ->
                translationX = beneathShift()
            key in lower && cover < 1f ->
                translationX = -toward * motion.parallax * w * cover
            else -> {
                alpha = 0f
                translationX = -2f * w
            }
        }
    }

    /**
     * Draws [key]'s entry. Of a lower entry in a slide, only the part the upper page has not reached
     * yet is drawn, under a scrim and the shadow the upper page's leading edge casts there; beneath
     * the back card, all of it is drawn under the card's scrim. Each is one rectangle, with no layer:
     * dimming the covered screen through its alpha would render all of it offscreen on every frame.
     *
     * The upper page is opaque, so the part of the lower one it covers is clipped away rather than
     * drawn and painted over. A transition starts while the GPU is still clocked down from idle, and
     * the covered part is most of the lower page on a pop's first frames.
     */
    fun ContentDrawScope.drawOver(key: Any, isLight: Boolean) {
        val revealing = card.active && key in card.beneath
        val c = cover
        if (key !in lower || c <= 0f || c >= 1f) {
            if (revealing && coveredByCard(key, cardHole)) {
                drawAroundCard(cardHole)
                return
            }
            drawContent()
            if (revealing) drawCardScrim()
            return
        }
        val w = width
        val left = lefts[key] ?: 0f
        // The upper page's leading edge, in this entry's coordinates.
        val shift = if (revealing) 1f - card.reveal else 1f
        val offset = -(if (rtl) -1f else 1f) * motion.parallax * w * c * shift
        val edge = if (rtl) w * c - left - offset else w * (1f - c) - left - offset
        val from = if (rtl) edge.coerceAtLeast(0f) else 0f
        val to = if (rtl) size.width else edge.coerceAtMost(size.width)
        when {
            // The card shrinks the upper page, so the whole lower page may show around it.
            revealing -> drawContent()
            // Whole pixels, so the clip is a scissor, and one more on the covered side, so no
            // rounding of the upper page's position opens a seam between the two.
            to > from -> clipRect(
                left = if (rtl) floor(from) - 1f else from,
                right = if (rtl) to else ceil(to) + 1f,
            ) { this@drawOver.drawContent() }
        }
        if (to > from) {
            drawRect(Color.Black, Offset(from, 0f), Size(to - from, size.height), alpha = coverScrimAlpha(isLight) * c)
        }
        val shadow = EdgeShadow.toPx()
        val brush = shadowBrushes.getOrPut(shadow.toInt() * if (rtl) -1 else 1) {
            val dark = Color.Black.copy(alpha = SHADOW_ALPHA)
            Brush.horizontalGradient(
                colors = if (rtl) listOf(dark, Color.Transparent) else listOf(Color.Transparent, dark),
                startX = 0f,
                endX = shadow,
            )
        }
        translate(left = if (rtl) edge else edge - shadow) {
            drawRect(brush, size = Size(shadow, size.height), alpha = c)
        }
        if (revealing) drawCardScrim()
    }

    /** Where the back card puts the page beneath it: the parallax a running push left it at, easing to rest. */
    private fun beneathShift(): Float = -(if (rtl) -1f else 1f) * motion.parallax * width * cover * (1f - card.reveal)

    /**
     * Writes into [hole] the part of [key]'s entry, in its own px, that the opaque back card hides
     * wholly: the card's rectangle less its corner radius at the top and the bottom, so the corners
     * are never in it. False while the card fades on commit, when everything under it shows.
     */
    private fun coveredByCard(key: Any, hole: MutableRect): Boolean {
        val subject = card.subject ?: return false
        if (card.contentAlpha.value < 1f) return false
        val w = width
        val h = height
        // The card is the subject's layer, scaled about its own centre, so its own width, not the host's.
        val cardWidth = widths[subject] ?: w
        val scale = card.scale
        val slide = if (subject in upper) (if (rtl) -1f else 1f) * w * (1f - cover) else 0f
        val originX = (lefts[key] ?: 0f) + beneathShift()
        val centerX = (lefts[subject] ?: 0f) + cardWidth / 2f + slide + with(card) { density.shiftX(cardWidth) } - originX
        val centerY = h / 2f + with(card) { density.shiftY(h) }
        val corner = with(card) { density.corner() } * scale
        hole.left = centerX - scale * cardWidth / 2f
        hole.right = centerX + scale * cardWidth / 2f
        hole.top = centerY - scale * h / 2f + corner
        hole.bottom = centerY + scale * h / 2f - corner
        return true
    }

    /**
     * The page beneath the back card, and the card's scrim over it, drawn only around [hole], in up to
     * four strips. The card is opaque, so what lies under it would be drawn and painted over on every
     * frame of the gesture: at its smallest the card still hides 81 % of the page, at its start all
     * of it, and each frame starts with the GPU at the clock the gesture found it at.
     */
    private fun ContentDrawScope.drawAroundCard(hole: MutableRect) {
        val w = size.width
        val h = size.height
        // Whole pixels, so every strip is a scissor; the hole shrinks to them, so the strips reach a
        // little under the card's edges and no rounding opens a seam beside it.
        val left = ceil(hole.left).coerceIn(0f, w)
        val right = floor(hole.right).coerceIn(left, w)
        val top = ceil(hole.top).coerceIn(0f, h)
        val bottom = floor(hole.bottom).coerceIn(top, h)
        val scrim = card.scrim.value
        drawStrip(0f, 0f, w, top, scrim)
        drawStrip(0f, bottom, w, h, scrim)
        drawStrip(0f, top, left, bottom, scrim)
        drawStrip(right, top, w, bottom, scrim)
    }

    private fun ContentDrawScope.drawStrip(left: Float, top: Float, right: Float, bottom: Float, scrim: Float) {
        if (right <= left || bottom <= top) return
        clipRect(left, top, right, bottom) { this@drawStrip.drawContent() }
        if (scrim > 0f) drawRect(Color.Black, Offset(left, top), Size(right - left, bottom - top), alpha = scrim)
    }

    private fun DrawScope.drawCardScrim() {
        val alpha = card.scrim.value
        if (alpha > 0f) drawRect(Color.Black, alpha = alpha)
    }

    private companion object {
        val EdgeShadow = 8.dp
        /** Faint, as UIKit's: a darker line at the edge reads as a seam, not depth. */
        const val SHADOW_ALPHA = 0.04f
    }
}

/** How dark the covered screen gets at the start of a push: lighter on a light theme, where black reads heavier. */
internal fun coverScrimAlpha(isLight: Boolean): Float = if (isLight) 0.10f else 0.24f
