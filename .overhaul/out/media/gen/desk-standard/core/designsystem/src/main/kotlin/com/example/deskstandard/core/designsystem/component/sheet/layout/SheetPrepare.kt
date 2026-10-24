package com.example.deskstandard.core.designsystem.component.sheet.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.deskstandard.core.designsystem.component.sheet.SheetController
import kotlinx.coroutines.flow.collectLatest

/**
 * How much of a hidden sheet's content is measured per frame while a press prepares it. A lazy list
 * composes only the rows that fit the room it is measured in, so raising the room a step per frame
 * spreads its rows over the frames of the press instead of the frame after the tap.
 */
internal object PrepareRamp {
    /** Before any step has been timed: a few rows, plus the sheet's own first composition, fit one frame. */
    val FirstStep = 160.dp
    val MinStep = 64.dp
    val MaxStep = 320.dp

    /** Half a 120 Hz frame for the rows, leaving the rest for the press feedback and the frame's own work. */
    const val BUDGET_NANOS = 4_000_000L

    /** The room for the next frame, from how much the last step added and how long its measure took. */
    fun next(room: Float, full: Float, lastNanos: Long, lastGain: Float, ended: Boolean, density: Density): Float =
        with(density) {
            when {
                ended -> full
                room.isNaN() -> minOf(full, FirstStep.toPx())
                else -> {
                    val step = (lastGain / maxOf(lastNanos, 1L) * BUDGET_NANOS).coerceIn(MinStep.toPx(), MaxStep.toPx())
                    minOf(full, room + step)
                }
            }
        }
}

/**
 * What the hidden measures of a prepared sheet found. [room] is read in measure, so raising it
 * measures the body again without recomposing anything; the rest is written by measure and read by the
 * ramp between frames, never in measure.
 */
internal class PrepareProgress {
    /** How tall the content may be measured this frame; NaN for the first step. */
    var room by mutableFloatStateOf(Float.NaN)

    /**
     * The content was last measured shorter than its room, so the stops may be a guess: the sheet
     * presents only after a full measure.
     */
    var partial by mutableStateOf(false)

    var full = 0f
        private set
    var measured = 0f
        private set
    var lastNanos = 0L
        private set
    var lastGain = 0f
        private set
    var ended = false
        private set
    var steps = 0
        private set

    /** The content is measured whole: at its full room, or it ended inside a smaller one. */
    val done: Boolean get() = steps > 0 && (ended || measured >= full)

    fun record(room: Float, full: Float, nanos: Long, ended: Boolean) {
        lastGain = room - measured
        measured = room
        this.full = full
        lastNanos = nanos
        this.ended = ended
        steps++
    }

    fun reset() {
        room = Float.NaN
        full = 0f
        measured = 0f
        lastNanos = 0L
        lastGain = 0f
        ended = false
        steps = 0
    }
}

/** Raises a prepared sheet's measured room one step per frame until its whole content is measured. */
@Composable
internal fun SheetPrepareEffect(controller: SheetController) {
    val density = LocalDensity.current
    LaunchedEffect(controller, density) {
        val state = controller.sheetState
        val progress = controller.prepare
        snapshotFlow { state.preparing && !state.isVisible }.collectLatest { ramping ->
            progress.reset()
            if (!ramping) return@collectLatest
            var seen = 0
            while (true) {
                // Resumes at the start of the next frame, before that frame's measure.
                withFrameNanos { }
                if (progress.steps == seen) continue
                seen = progress.steps
                if (progress.done) break
                progress.room = PrepareRamp.next(
                    room = progress.measured,
                    full = progress.full,
                    lastNanos = progress.lastNanos,
                    lastGain = progress.lastGain,
                    ended = progress.ended,
                    density = density,
                )
            }
        }
    }
}
